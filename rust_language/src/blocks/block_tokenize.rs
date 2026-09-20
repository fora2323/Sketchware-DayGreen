pub struct TokenParser {
    src: Vec<u8>,
    i: usize,
}

impl TokenParser {
    pub fn new_str(input: &str) -> Self {
        Self {
            src: input.as_bytes().to_vec(),
            i: 0,
        }
    }

    pub fn new_bytes(bytes: &[u8]) -> Self {
        Self {
            src: bytes.to_vec(),
            i: 0,
        }
    }

    fn at_end(&self) -> bool {
        self.i >= self.src.len()
    }

    fn skip_ws(&mut self) {
        while self.i < self.src.len() && self.src[self.i] == b' ' {
            self.i += 1;
        }
    }

    fn next_token(&mut self) -> Option<(usize, usize)> {
        self.skip_ws();
        if self.at_end() {
            return None;
        }
        let start = self.i;
        let mut special = false;

        while self.i < self.src.len() && self.src[self.i] != b' ' {
            let c = self.src[self.i];
            if c == b'\\' && self.i + 1 < self.src.len() {
                self.i += 2;
                continue;
            }
            if c == b'%' {
                if self.i > start {
                    break;
                }
                special = true;
            }
            if special && (c == b'?' || c == b'-') {
                break;
            }
            self.i += 1;
        }

        Some((start, self.i - start))
    }

    pub fn parse_str(input: &str) -> Vec<String> {
        let mut p = TokenParser::new_str(input);
        let mut out = Vec::new();
        while let Some((s, l)) = p.next_token() {
            if l > 0 {
                let slice = &p.src[s..s + l];
                out.push(String::from_utf8_lossy(slice).to_string());
            }
        }
        out
    }

    pub fn parse_bytes(bytes: &[u8]) -> Vec<(usize, usize)> {
        let mut p = TokenParser::new_bytes(bytes);
        let mut out = Vec::new();
        while let Some((s, l)) = p.next_token() {
            if l > 0 {
                out.push((s, l));
            }
        }
        out
    }
}

use jni::objects::{JByteArray, JClass, JObject};
use jni::sys::jobjectArray;
use jni::JNIEnv;

#[unsafe(no_mangle)]
pub extern "C" fn Java_io_edward_blockcraft_core_BlocksLoader_nativeBlockTokenizer(
    mut env: JNIEnv,
    _class: JClass,
    input: JByteArray,
) -> jobjectArray {
    let bytes = env.convert_byte_array(input).unwrap();
    let ranges = TokenParser::parse_bytes(&bytes);

    let string_class = env.find_class("java/lang/String").unwrap();
    let arr = env
        .new_object_array(ranges.len() as i32, string_class, JObject::null())
        .unwrap();

    for (idx, (start, len)) in ranges.iter().enumerate() {
        let s = std::str::from_utf8(&bytes[*start..*start + *len]).unwrap_or("");
        let jstr = env.new_string(s).unwrap();
        env.set_object_array_element(&arr, idx as i32, JObject::from(jstr))
            .unwrap();
    }

    **arr
}
