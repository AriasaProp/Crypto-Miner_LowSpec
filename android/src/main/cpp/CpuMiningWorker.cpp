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

static jclass m_class;
static jclass floatClass;
static jmethodID invokeNonce;
static jmethodID msl_sendMessage;
static jmethodID floatConstructor;

static bool doingJob = false;

static uint32_t job_step;
static jobject job_globalClass;
static uint8_t job_header[76];
static uint8_t job_target[SHA256_HASH_SIZE];

static std::chrono::high_resolution_clock time_saved;
static pthread_mutex_t _mtx = PTHREAD_MUTEX_INITIALIZER;
static pthread_cond_t mcond = PTHREAD_COND_INITIALIZER;

static inline bool checkWithGuard(bool *check) {
    pthread_mutex_lock(&_mtx);
    bool r = *check;
    pthread_mutex_unlock(&_mtx);
    return r;
}

static unsigned long hash_total;
static unsigned long hash_sec;
std::chrono::steady_clock::time_point saved_time;

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
                    pthread_mutex_unlock(&_mtx);
                    //java methode
                    JNIEnv *env;
                    if (global_jvm->AttachCurrentThread(&env, &attachArgs) == JNI_OK) {
                        env->CallVoidMethod(job_globalClass, invokeNonce, nonce);
                        global_jvm->DetachCurrentThread();
                    }
                    //....
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
                jobject speed_calcl = env->NewObject(floatClass, floatConstructor, speed);
                env->CallVoidMethod(job_globalClass, msl_sendMessage, MSG_UPDATE, MSG_UPDATE_SPEED, 0, speed_calcl);
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

static pthread_t *workers = nullptr;

void stopJob() {
    if (!workers) return;
    pthread_mutex_lock(&_mtx);
    doingJob = false;
    pthread_mutex_unlock(&_mtx);
    for (size_t i = 0; i < job_step; i++) {
        pthread_join(workers[i], nullptr);
    }
    delete[] workers;
    workers = nullptr;
}

void doJob(uint32_t parallel, char* header, char* target) {
    stopJob();
    workers = new pthread_t[parallel];
    pthread_mutex_lock(&_mtx);
    saved_time = std::chrono::steady_clock::now();
    hash_total = 0;
    hash_sec = 0;
    job_step = parallel;
    doingJob = true;
    memcpy(job_header, header, 76);
    memcpy(job_target, target, SHA256_HASH_SIZE);
    pthread_mutex_unlock(&_mtx);
    pthread_attr_t attr;
    pthread_attr_init (&attr);
    pthread_attr_setdetachstate (&attr, PTHREAD_CREATE_JOINABLE);
    for(uint32_t i = 0; i < parallel; i++) {
        pthread_create(workers+i, &attr, hasher, (void*)&i);
    }
    pthread_attr_destroy (&attr);
}

bool onload_CpyMiningWorker(JNIEnv *env) {
    m_class = env->FindClass("com/ariasaproject/cmls/worker/CpuMiningWorker");
    if (!m_class) goto failed_section;
    invokeNonce = env->GetMethodID(m_class, "invokeNonceFound", "(I)V");
    if (!invokeNonce) goto failed_section;
    msl_sendMessage = env->GetMethodID(m_class, "msl_sendMessage", "(IIILjava/lang/Object;)V");
    if (!msl_sendMessage) goto failed_section;
    floatClass = env->FindClass("java/lang/Float");
    if (!floatClass) goto failed_section;
    floatConstructor = env->GetMethodID(floatClass, "<init>", "(F)V");
    if (!floatConstructor) goto failed_section;
    
    return true;
failed_section:
    return false;
}
void onunload_CpyMiningWorker(JNIEnv *) {
    m_class = NULL;
    floatClass = NULL;
    invokeNonce = NULL;
    msl_sendMessage = NULL;
    floatConstructor = NULL;
}

#define JNIF(R, M) extern "C" JNIEXPORT R JNICALL Java_com_ariasaproject_cmls_worker_CpuMiningWorker_##M

JNIF(jboolean, nativeJob) (JNIEnv *env, jobject o, jint step, jbyteArray h, jbyteArray t) {
    //speed update
    jobject speed_calcl = env->NewObject(floatClass, floatConstructor, 0);
    env->CallVoidMethod(o, msl_sendMessage, MSG_UPDATE, MSG_UPDATE_SPEED, 0, speed_calcl);
    // ....
    job_globalClass = env->NewGlobalRef(o);
    jbyte* header = env->GetByteArrayElements(h, NULL);
    jbyte* target = env->GetByteArrayElements(t, NULL);
    doJob(step, (char*) header, (char*) target);
    env->ReleaseByteArrayElements(t, target, JNI_ABORT);
    env->ReleaseByteArrayElements(h, header, JNI_ABORT);
}
JNIF(void, nativeStop) (JNIEnv *env, jobject) {
    stopJob();
    env->DeleteGlobalRef(job_globalClass);
    job_globalClass = NULL;
}
JNIF(jint, getHashesTotal) (JNIEnv *, jobject) {
    return hash_total;
}
JNIF(jboolean, getStatus) (JNIEnv *, jobject) {
    return doingJob;
}
