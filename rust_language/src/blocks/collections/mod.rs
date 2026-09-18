use crate::blocks::block_palette::BlockBean;
use crate::blocks::generators::java::java_blocks_generator::JavaBlocksGenerator;
use crate::blocks::generators::xml::xml_blocks_generator::XmlBlocksGenerator;

use jni::objects::{JClass, JString};
use jni::sys::jstring;
use jni::JNIEnv;
use serde_json::{json, Value};
use std::fs;

pub mod java;
pub mod xml;

pub fn parse_to_json(blocks: Vec<BlockBean>) -> String {
    let json_array: Vec<Value> = blocks.iter().map(|b| b.full_json()).collect();
    serde_json::to_string(&json_array)
        .map_err(|e| e.to_string())
        .unwrap()
}

#[allow(non_snake_case)]
#[unsafe(no_mangle)]
pub extern "C" fn Java_io_edward_blockcraft_core_BlocksLoader_BlockBeansGenerator(
    mut env: JNIEnv,
    _class: JClass,
    file_path: JString,
) -> jstring {
    let binding = env.get_string(&file_path).unwrap();
    let path_str = binding.to_str().unwrap();

    let file_content = fs::read_to_string(path_str).unwrap();

    let generated_blocks = if path_str.ends_with(".java") {
        JavaBlocksGenerator::new(file_content).generate()
    } else {
        XmlBlocksGenerator::new(file_content).generate()
    };

    let json_result = match generated_blocks {
        Ok(blocks) => parse_to_json(blocks),
        Err(error) => json!({ "Error": error }).to_string(),
    };

    env.new_string(json_result).unwrap().into_raw()
}

#[allow(non_snake_case)]
#[unsafe(no_mangle)]
pub extern "C" fn Java_io_edward_blockcraft_core_BlocksLoader_generateBlocksFromCode(
    mut env: JNIEnv,
    _class: JClass,
    code: JString,
) -> jstring {
    let binding = match env.get_string(&code) {
        Ok(s) => s,
        Err(e) => {
            let err_json = json!({ "Error": format!("Failed to read JNI string: {:?}", e) }).to_string();
            return env.new_string(err_json).unwrap().into_raw();
        }
    };
    let code_str = match binding.to_str() {
        Ok(s) => s,
        Err(e) => {
            let err_json = json!({ "Error": format!("Invalid UTF-8: {:?}", e) }).to_string();
            return env.new_string(err_json).unwrap().into_raw();
        }
    };

    let generated_blocks = JavaBlocksGenerator::new(code_str.to_string()).generate();

    let json_result = match generated_blocks {
        Ok(blocks) => parse_to_json(blocks),
        Err(error) => json!({ "Error": error }).to_string(),
    };

    env.new_string(json_result).unwrap().into_raw()
}
