use jni::objects::JClass;
use jni::sys::jstring;
use jni::JNIEnv;
use serde_json::{to_string, Map, Value};

use crate::blocks::block_palette::BlockPalette;
use crate::blocks::collections::{
    java::{
        control::Control as JavaControl,
        generator_collection::GeneratorCollection as JavaGeneratorCollection,
        operations::Operators as JavaOperations,
    },
    xml::{
        general_attr::GeneralAttrs as XmlGeneralAttr,
        general_views::GeneralViews as XmlGeneralViews,
    },
};

pub fn load_all_langs_json() -> Value {
    let mut obj = Map::new();
    let langs = vec!["java", "xml"];

    for l in langs {
        let palettes = load_lang(l);
        let arr: Vec<Value> = palettes.iter().map(|p| p.to_json()).collect();
        obj.insert(l.to_string(), Value::Array(arr));
    }

    Value::Object(obj)
}

pub fn load_lang(language: &str) -> Vec<BlockPalette> {
    let mut vec = Vec::new();

    if language == "java" {
        vec.push(JavaControl::load());
        vec.push(JavaOperations::load());
        vec.push(JavaGeneratorCollection::load());
    } else if language == "xml" {
        vec.push(XmlGeneralAttr::load());
        vec.push(XmlGeneralViews::load());
    }

    vec
}

#[unsafe(no_mangle)]
pub extern "C" fn Java_io_edward_blockcraft_core_BlocksLoader_loadBlocksCollection(
    env: JNIEnv,
    _class: JClass,
) -> jstring {
    let json_value = load_all_langs_json();
    let json_string = to_string(&json_value).unwrap_or_else(|_| "{}".to_string());
    env.new_string(json_string).unwrap().into_raw()
}
