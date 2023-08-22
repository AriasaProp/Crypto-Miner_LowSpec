#ifndef JSON_H
#define JSON_H

#include <cctype>
#include <cstdio>
#include <cstdlib>
#include <stdexcept>
#include <string>
#include <utility>
#include <vector>

namespace json {

class invalid_key : public std::exception {
public:
  const std::string key;

  inline invalid_key (const std::string &key) : key (key) {}

  inline virtual ~invalid_key () throw () {}

  virtual const char *what () const throw () {
    return key.c_str ();
  }
};

class parsing_error : public std::invalid_argument {
public:
  inline parsing_error (const char *message) : std::invalid_argument (message) {}

  inline virtual ~parsing_error () throw () {}
};

typedef std::vector<std::string> key_list_t;

namespace jtype {

  enum jtype {
    jstring,
    jnumber,
    jobject,
    jarray,
    jbool,
    jnull,
    not_valid
  };

  jtype peek (const char input);

  jtype detect (const char *input);
} // namespace jtype

class reader : protected std::string {
public:
  enum push_result {
    ACCEPTED,
    REJECTED,
    WHITESPACE
  };

  inline reader () : std::string (), sub_reader (NULL) { this->clear (); }

  virtual void clear ();

  using std::string::length;

#if __GNUC__ && __GNUC__ < 11
  inline char front () const { return this->at (0); }
  inline char back () const { return this->at (this->length () - 1); }
#else

  using std::string::front;

  using std::string::back;
#endif

  virtual push_result push (const char next);

  inline virtual jtype::jtype type () const {
    return this->length () > 0 ? jtype::peek (this->front ()) : json::jtype::not_valid;
  }

  virtual bool is_valid () const;

  inline virtual std::string readout () const { return *this; }

  inline virtual ~reader () { this->clear (); }

protected:
  reader *sub_reader;

  push_result push_string (const char next);

  push_result push_array (const char next);

  push_result push_object (const char next);

  push_result push_number (const char next);

  push_result push_boolean (const char next);

  push_result push_null (const char next);

  template <typename T>
  T get_state () const {
    return static_cast<T> (this->read_state);
  }

  template <typename T>
  void set_state (const T state) {
    this->read_state = (char)state;
  }

  enum string_reader_enum {
    STRING_EMPTY = 0,
    STRING_OPENING_QUOTE,
    STRING_OPEN,
    STRING_ESCAPED,
    STRING_CODE_POINT_START,
    STRING_CODE_POINT_1,
    STRING_CODE_POINT_2,
    STRING_CODE_POINT_3,
    STRING_CLOSED
  };

  enum number_reader_enum {
    NUMBER_EMPTY = 0,
    NUMBER_OPEN_NEGATIVE,
    NUMBER_ZERO,
    NUMBER_INTEGER_DIGITS,
    NUMBER_DECIMAL,
    NUMBER_FRACTION_DIGITS,
    NUMBER_EXPONENT,
    NUMBER_EXPONENT_SIGN,
    NUMBER_EXPONENT_DIGITS
  };

  enum array_reader_enum {
    ARRAY_EMPTY = 0,
    ARRAY_OPEN_BRACKET,
    ARRAY_READING_VALUE,
    ARRAY_AWAITING_NEXT_LINE,
    ARRAY_CLOSED
  };

  enum object_reader_enum {
    OBJECT_EMPTY = 0,
    OBJECT_OPEN_BRACE,
    OBJECT_READING_ENTRY,
    OBJECT_AWAITING_NEXT_LINE,
    OBJECT_CLOSED
  };

private:
  char read_state;
};

class kvp_reader : public reader {
public:
  inline kvp_reader () : reader () {
    this->clear ();
  }

  inline virtual void clear () {
    reader::clear ();
    this->_key.clear ();
    this->_colon_read = false;
  }

  virtual push_result push (const char next);

  inline virtual bool is_valid () const {
    return reader::is_valid () && this->_key.is_valid ();
  }

  virtual std::string readout () const;

private:
  reader _key;

  bool _colon_read;
};

namespace parsing {

  const char *tlws (const char *start);

  std::string read_digits (const char *input);

  std::string decode_string (const char *input);

  std::string encode_string (const char *input);

  struct parse_results {

    jtype::jtype type;

    std::string value;

    const char *remainder;
  };

  parse_results parse (const char *input);

  template <typename T>
  T get_number (const char *input, const char *format) {
    T result;
    std::sscanf (input, format, &result);
    return result;
  }

  template <typename T>
  std::string get_number_string (const T &number, const char *format) {
    std::vector<char> cstr (6);
    int remainder = std::snprintf (&cstr[0], cstr.size (), format, number);
    if (remainder < 0) {
      return std::string ();
    } else if (remainder >= (int)cstr.size ()) {
      cstr.resize (remainder + 1);
      std::snprintf (&cstr[0], cstr.size (), format, number);
    }
    std::string result (&cstr[0]);
    return result;
  }

  std::vector<std::string> parse_array (const char *input);
} // namespace parsing

typedef std::pair<std::string, std::string> kvp;

class jobject {
private:
  std::vector<kvp> data;

  bool array_flag;

public:
  inline jobject (bool array = false)
      : array_flag (array) {}

  inline jobject (const jobject &other)
      : data (other.data),
        array_flag (other.array_flag) {}

  inline virtual ~jobject () {}

  bool is_array () const { return this->array_flag; }

  inline size_t size () const { return this->data.size (); }

  inline void clear () { this->data.resize (0); }

  bool operator== (const json::jobject other) const { return ((std::string) (*this)) == (std::string)other; }

  bool operator!= (const json::jobject other) const { return ((std::string) (*this)) != (std::string)other; }

  inline jobject &operator= (const jobject rhs) {
    this->array_flag = rhs.array_flag;
    this->data = rhs.data;
    return *this;
  }

  jobject &operator+= (const kvp &other) {
    if (!this->array_flag && this->has_key (other.first)) throw json::parsing_error ("Key conflict");
    if (this->array_flag && other.first != "") throw json::parsing_error ("Array cannot have key");
    if (!this->array_flag && other.first == "") throw json::parsing_error ("Missing key");
    this->data.push_back (other);
    return *this;
  }

  jobject &operator+= (const jobject &other) {
    if (this->array_flag != other.array_flag) throw json::parsing_error ("Array/object mismatch");
    json::jobject copy (other);
    for (size_t i = 0; i < copy.size (); i++) {
      this->operator+= (copy.data.at (i));
    }
    return *this;
  }

  jobject operator+ (jobject &other) {
    jobject result = *this;
    result += other;
    return result;
  }

  static jobject parse (const char *input);

  static inline jobject parse (const std::string input) { return parse (input.c_str ()); }

  inline bool static tryparse (const char *input, jobject &output) {
    try {
      output = parse (input);
    } catch (...) {
      return false;
    }
    return true;
  }

  inline bool has_key (const std::string &key) const {
    if (this->array_flag) return false;
    for (size_t i = 0; i < this->size (); i++)
      if (this->data.at (i).first == key) return true;
    return false;
  }

  key_list_t list_keys () const;

  void set (const std::string &key, const std::string &value);

  inline std::string get (const size_t index) const {
    return this->data.at (index).second;
  }

  inline std::string get (const std::string &key) const {
    if (this->array_flag) throw json::invalid_key (key);
    for (size_t i = 0; i < this->size (); i++)
      if (this->data.at (i).first == key) return this->get (i);
    throw json::invalid_key (key);
  }

  void remove (const std::string &key);

  void remove (const size_t index) {
    this->data.erase (this->data.begin () + index);
  }

  class entry {
  protected:
    virtual const std::string &ref () const = 0;

    template <typename T>
    inline T get_number (const char *format) const {
      return json::parsing::get_number<T> (this->ref ().c_str (), format);
    }

    template <typename T>
    inline std::vector<T> get_number_array (const char *format) const {
      std::vector<std::string> numbers = json::parsing::parse_array (this->ref ().c_str ());
      std::vector<T> result;
      for (size_t i = 0; i < numbers.size (); i++) {
        result.push_back (json::parsing::get_number<T> (numbers[i].c_str (), format));
      }
      return result;
    }

  public:
    inline std::string as_string () const {
      return json::jtype::peek (*this->ref ().c_str ()) == json::jtype::jstring ? json::parsing::decode_string (this->ref ().c_str ()) : this->ref ();
    }

    inline operator std::string () const {
      return this->as_string ();
    }

    bool operator== (const std::string other) const { return ((std::string) (*this)) == other; }

    bool operator!= (const std::string other) const { return !(((std::string) (*this)) == other); }

    operator int () const;

    operator unsigned int () const;

    operator long () const;

    operator unsigned long () const;

    operator char () const;

    operator float () const;

    operator double () const;

    inline json::jobject as_object () const {
      return json::jobject::parse (this->ref ().c_str ());
    }

    inline operator json::jobject () const {
      return this->as_object ();
    }

    operator std::vector<int> () const;

    operator std::vector<unsigned int> () const;

    operator std::vector<long> () const;

    operator std::vector<unsigned long> () const;

    operator std::vector<char> () const;

    operator std::vector<float> () const;

    operator std::vector<double> () const;

    operator std::vector<json::jobject> () const {
      const std::vector<std::string> objs = json::parsing::parse_array (this->ref ().c_str ());
      std::vector<json::jobject> results;
      for (size_t i = 0; i < objs.size (); i++) {
        results.push_back (json::jobject::parse (objs[i].c_str ()));
      }
      return results;
    }

    operator std::vector<std::string> () const { return json::parsing::parse_array (this->ref ().c_str ()); }

    template <typename T>
    inline std::vector<T> as_array () const {
      return (std::vector<T>)(*this);
    }

    inline bool is_string () const {
      return json::parsing::parse (this->ref ().c_str ()).type == json::jtype::jstring;
    }

    inline bool is_number () const {
      return json::parsing::parse (this->ref ().c_str ()).type == json::jtype::jnumber;
    }

    inline bool is_object () const {
      const jtype::jtype type = json::parsing::parse (this->ref ().c_str ()).type;
      return type == json::jtype::jobject || type == json::jtype::jarray;
    }

    inline bool is_array () const {
      return json::parsing::parse (this->ref ().c_str ()).type == json::jtype::jarray;
    }

    inline bool is_bool () const {
      return json::parsing::parse (this->ref ().c_str ()).type == json::jtype::jbool;
    }

    inline bool is_true () const {
      json::parsing::parse_results result = json::parsing::parse (this->ref ().c_str ());
      return (result.type == json::jtype::jbool && result.value == "true");
    }

    inline bool is_null () const {
      return json::parsing::parse (this->ref ().c_str ()).type == json::jtype::jnull;
    }
  };

  class const_value : public entry {
  private:
    std::string data;

  protected:
    inline const std::string &ref () const {
      return this->data;
    }

  public:
    inline const_value (std::string value)
        : data (value) {}

    inline const_value get (const std::string &key) const {
      return const_value (json::jobject::parse (this->data).get (key));
    }

    inline const_value array (const size_t index) const {
      return const_value (json::jobject::parse (this->data).get (index));
    }
  };

  class const_proxy : public entry {
  private:
    const jobject &source;

  protected:
    const std::string key;

    inline const std::string &ref () const {
      for (size_t i = 0; i < this->source.size (); i++)
        if (this->source.data.at (i).first == key) return this->source.data.at (i).second;
      throw json::invalid_key (key);
    }

  public:
    const_proxy (const jobject &source, const std::string key) : source (source), key (key) {
      if (source.array_flag) throw std::logic_error ("Source cannot be an array");
    }

    const_value array (size_t index) const {
      const char *value = this->ref ().c_str ();
      if (json::jtype::peek (*value) != json::jtype::jarray)
        throw std::invalid_argument ("Input is not an array");
      const std::vector<std::string> values = json::parsing::parse_array (value);
      return const_value (values[index]);
    }
  };

  class proxy : public json::jobject::const_proxy {
  private:
    jobject &sink;

  protected:
    template <typename T>
    inline void set_number (const T value, const char *format) {
      this->sink.set (key, json::parsing::get_number_string (value, format));
    }

    void set_array (const std::vector<std::string> &values, const bool wrap = false);

    template <typename T>
    inline void set_number_array (const std::vector<T> &values, const char *format) {
      std::vector<std::string> numbers;
      for (size_t i = 0; i < values.size (); i++) {
        numbers.push_back (json::parsing::get_number_string (values[i], format));
      }
      this->set_array (numbers);
    }

  public:
    proxy (jobject &source, const std::string key)
        : json::jobject::const_proxy (source, key),
          sink (source) {}

    inline void operator= (const std::string value) {
      this->sink.set (this->key, json::parsing::encode_string (value.c_str ()));
    }

    inline void operator= (const char *value) {
      this->operator= (std::string (value));
    }

    void operator= (const int input) { this->set_number (input, "%i"); }

    void operator= (const unsigned int input) { this->set_number (input, "%u"); }

    void operator= (const long input) { this->set_number (input, "%li"); }

    void operator= (const unsigned long input) { this->set_number (input, "%lu"); }

    void operator= (const char input) { this->set_number (input, "%c"); }

    void operator= (const double input) { this->set_number (input, "%e"); }

    void operator= (const float input) { this->set_number (input, "%e"); }

    void operator= (json::jobject input) {
      this->sink.set (key, (std::string)input);
    }

    void operator= (const std::vector<int> input) { this->set_number_array (input, "%i"); }

    void operator= (const std::vector<unsigned int> input) { this->set_number_array (input, "%u"); }

    void operator= (const std::vector<long> input) { this->set_number_array (input, "%li"); }

    void operator= (const std::vector<unsigned long> input) { this->set_number_array (input, "%lu"); }

    void operator= (const std::vector<char> input) { this->set_number_array (input, "%c"); }

    void operator= (const std::vector<float> input) { this->set_number_array (input, "%e"); }

    void operator= (const std::vector<double> input) { this->set_number_array (input, "%e"); }

    void operator= (const std::vector<std::string> input) { this->set_array (input, true); }

    void operator= (const std::vector<json::jobject> input) {
      std::vector<std::string> objs;
      for (size_t i = 0; i < input.size (); i++) {
        objs.push_back ((std::string)input[i]);
      }
      this->set_array (objs, false);
    }

    inline void set_boolean (const bool value) {
      if (value)
        this->sink.set (key, "true");
      else
        this->sink.set (key, "false");
    }

    inline void set_null () {
      this->sink.set (key, "null");
    }

    inline void clear () {
      this->sink.remove (key);
    }
  };

  inline virtual jobject::proxy operator[] (const std::string key) {
    if (this->array_flag) throw json::invalid_key (key);
    return jobject::proxy (*this, key);
  }

  inline virtual const jobject::const_proxy operator[] (const std::string key) const {
    if (this->array_flag) throw json::invalid_key (key);
    return jobject::const_proxy (*this, key);
  }

  inline const jobject::const_value array (const size_t index) const {
    return jobject::const_value (this->data.at (index).second);
  }

  operator std::string () const;

  inline std::string as_string () const {
    return this->operator std::string ();
  }

  std::string pretty (unsigned int indent_level = 0) const;
};
} // namespace json

#endif