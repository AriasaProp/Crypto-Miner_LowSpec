#include "hashing.hpp"

#include <pthread.h>
#include <jni.h>
#include <cstring>
#include <string>
#include <cstdint>
#include <endian.h>
#include <chrono>

#define MSG_STATE 1
#define MSG_UPDATE 2

#define MSG_STATE_NONE 0
#define MSG_STATE_ONSTART 1
#define MSG_STATE_RUNNING 2
#define MSG_STATE_ONSTOP 3

#define MSG_UPDATE_SPEED 1
#define MSG_UPDATE_ACCEPTED 2
#define MSG_UPDATE_REJECTED 3
#define MSG_UPDATE_STATUS 4
#define MSG_UPDATE_CONSOLE 5

extern JavaVM *global_jvm;


static JavaVMAttachArgs attachArgs {
    .version = JNI_VERSION_1_6,
    .name = "CpuWorker",
    .group = NULL
};

static jmethodID invokeNonce;
static jmethodID updateSpeed;
static jmethodID updateConsole;

static bool doingJob = false;

static uint32_t job_step;
static jobject job_globalClass;
static uint8_t job_header[76];
static uint8_t job_target[SHA256_HASH_SIZE];

static pthread_mutex_t _mtx = PTHREAD_MUTEX_INITIALIZER;
//static pthread_cond_t mcond = PTHREAD_COND_INITIALIZER;

static inline bool checkWithGuard(bool *check) {
    pthread_mutex_lock(&_mtx);
    bool r = *check;
    pthread_mutex_unlock(&_mtx);
    return r;
}

static unsigned long hash_total;
static unsigned long hash_sec;
static std::chrono::steady_clock::time_point saved_time;

static pthread_t *workers = nullptr;

bool onload_CpuMiningWorker(JNIEnv *env) {
    jclass m_class = env->FindClass("com/ariasaproject/cmls/worker/CpuMiningWorker");
    if (!m_class) goto failed_section;
    invokeNonce = env->GetMethodID(m_class, "invokeNonceFound", "(I)V");
    if (!invokeNonce) goto failed_section;
    updateSpeed = env->GetMethodID(m_class, "updateSpeed", "(F)V");
    if (!updateSpeed) goto failed_section;
    updateConsole = env->GetMethodID(m_class, "updateConsole", "(Ljava/lang/String;)V");
    if (!updateConsole) goto failed_section;
    
    return true;
failed_section:
    return false;
}
void onunload_CpuMiningWorker(JNIEnv *) {
    invokeNonce = NULL;
    updateSpeed = NULL;
    updateConsole = NULL;
}

static void *hasher(void *param) {
    uint32_t nonce = *((uint32_t*)param);
    hashing hp;
    do {
        hp.hash(job_header, nonce);
        uint8_t *tar = job_target, *res = hp.H;
        size_t i = SHA256_HASH_SIZE;
        while (--i) {
            uint8_t a = res[i], b = tar[i];
            if (a != b) {
                if (a < b) {
                    pthread_mutex_lock(&_mtx);
                    doingJob = false;
                    //java methode
                    JNIEnv *env;
                    if (global_jvm->AttachCurrentThread(&env, &attachArgs) == JNI_OK) {
                        env->CallVoidMethod(job_globalClass, invokeNonce, nonce);
                        global_jvm->DetachCurrentThread();
                    }
                    //....
                    pthread_mutex_unlock(&_mtx);
                    pthread_exit(nullptr);
                }
                break;
            }
        }
        
        //calculate speed
        pthread_mutex_lock(&_mtx);
        hash_total++;
        hash_sec++;
        std::chrono::steady_clock::time_point current_time = std::chrono::steady_clock::now();
        float timed = std::chrono::duration_cast<std::chrono::duration<float>>(current_time - saved_time).count();
        if (timed >= 1.0f) {
            JNIEnv *env;
            if (global_jvm->AttachCurrentThread(&env, &attachArgs) == JNI_OK) {
                float speed = (float)hash_sec / timed;
                env->CallVoidMethod(job_globalClass, updateSpeed, speed);
                global_jvm->DetachCurrentThread();
            }
            hash_sec = 0;
            saved_time = current_time;
        }
        pthread_mutex_unlock(&_mtx);
    
        //next nonce
        size_t next_nonce = nonce + job_step;
        if (next_nonce < nonce)
            break;
        nonce = next_nonce;
    } while (checkWithGuard(&doingJob));
    pthread_exit(nullptr);
}

#define JNIF(R, M) extern "C" JNIEXPORT R JNICALL Java_com_ariasaproject_cmls_worker_CpuMiningWorker_##M

JNIF(jboolean, nativeJob) (JNIEnv *env, jobject o, jint step, jbyteArray h, jbyteArray t) {
    //speed update
    env->CallVoidMethod(o, updateSpeed, 0.0f);
    env->CallVoidMethod(o, updateConsole, env->NewStringUTF("Native Worker: Workers Starting"));
    // ....
    job_globalClass = env->NewGlobalRef(o);
    jbyte* header = env->GetByteArrayElements(h, NULL);
    jbyte* target = env->GetByteArrayElements(t, NULL);
    // ...
    pthread_mutex_lock(&_mtx);
    if (workers) {
        doingJob = false;
        for (size_t i = 0; i < job_step; i++) {
            pthread_join(workers[i], nullptr);
        }
        delete[] workers;
        workers = nullptr;
    }
    saved_time = std::chrono::steady_clock::now();
    hash_total = 0;
    hash_sec = 0;
    job_step = static_cast<uint32_t>(step);
    doingJob = true;
    memcpy(job_header, header, 76);
    memcpy(job_target, target, SHA256_HASH_SIZE);
    pthread_mutex_unlock(&_mtx);
    env->ReleaseByteArrayElements(t, target, JNI_ABORT);
    env->ReleaseByteArrayElements(h, header, JNI_ABORT);
    
    workers = new pthread_t[job_step];
    pthread_attr_t attr;
    pthread_attr_init (&attr);
    pthread_attr_setdetachstate (&attr, PTHREAD_CREATE_JOINABLE);
    for(uint32_t i = 0; i < job_step; i++) {
        if(pthread_create(workers+i, &attr, hasher, (void*)&i) != 0)
            return false;
    }
    pthread_attr_destroy (&attr);
    env->CallVoidMethod(o, updateConsole, env->NewStringUTF("Native Worker: Workers Started"));
    //...
    return true;
}
JNIF(void, nativeStop) (JNIEnv *env, jobject o) {
    env->CallVoidMethod(o, updateConsole, env->NewStringUTF("Native Worker: Workers Stopping"));
    if (workers) {
        pthread_mutex_lock(&_mtx);
        doingJob = false;
        pthread_mutex_unlock(&_mtx);
        for (size_t i = 0; i < job_step; i++) {
            pthread_join(workers[i], nullptr);
        }
        delete[] workers;
        workers = nullptr;
    }
    env->DeleteGlobalRef(job_globalClass);
    job_globalClass = NULL;
    env->CallVoidMethod(o, updateConsole, env->NewStringUTF("Native Worker: Workers Stopped"));
}
JNIF(jint, getHashesTotal) (JNIEnv *, jobject) {
    return hash_total;
}
JNIF(jboolean, getStatus) (JNIEnv *, jobject) {
    return doingJob;
}
