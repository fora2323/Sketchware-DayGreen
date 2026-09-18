use serde_json::{json, Value};

use crate::blocks::block_tokenize::TokenParser;

pub struct BlockPalette {
    name: String,
    color: u32,
    pub beans: Vec<BlockBean>,
}

impl BlockPalette {
    pub fn new(name: &str, color: u32) -> Self {
        Self {
            name: name.to_string(),
            color,
            beans: Vec::new(),
        }
    }
    pub fn push(&mut self, mut bean: BlockBean) {
        if bean.color.is_none() {
            bean.color = Some(self.color)
        }
        self.beans.push(bean);
    }

    pub fn to_json(&self) -> Value {
        json!({
            "name": self.name,
            "color": self.color,
            "beans": self.beans.iter().map(|b| b.to_json_collection()).collect::<Vec<_>>()
        })
    }
}

#[derive(Clone, Debug)]
pub struct BlockBean {
    color: Option<u32>,
    pub id: i32,
    pub next_block: i32,
    pub(crate) op_code: String,
    param_types: Vec<String>,
    pub parameters: Vec<String>,
    pub tokenized_spec: Vec<String>,
    pub(crate) spec: String,
    pub sub_stack1: i32,
    pub sub_stack2: i32,
    block_type: String,
    pub(crate) code: String,
    spec2: String,
    header_text: String,
}

impl BlockBean {
    pub fn new() -> Self {
        Self {
            color: None,
            id: -1,
            next_block: -1,
            op_code: String::new(),
            param_types: Vec::new(),
            spec: String::new(),
            parameters: Vec::new(),
            tokenized_spec: Vec::new(),
            sub_stack1: -1,
            sub_stack2: -1,
            block_type: String::new(),
            code: String::new(),
            spec2: String::new(),
            header_text: String::new(),
        }
    }

    pub fn set_op_code(mut self, v: &str) -> Self {
        self.op_code = v.to_string();
        self
    }

    pub fn push_parameter(mut self, parameter: String) -> Self {
        self.parameters.push(parameter);
        self
    }

    pub fn set_spec(mut self, v: &str) -> Self {
        self.spec = v.to_string();

        self.tokenized_spec = TokenParser::parse_str(&self.spec);
        self
    }

    pub fn set_type(mut self, v: &str) -> Self {
        self.block_type = v.to_string();
        self
    }

    pub fn set_code(mut self, v: &str) -> Self {
        self.code = v.to_string();
        self
    }

    pub fn set_spec2(mut self, v: &str) -> Self {
        self.spec2 = v.to_string();
        self
    }

    pub fn set_header_text(mut self, v: &str) -> Self {
        self.header_text = v.to_string();
        self
    }

    pub fn set_color(mut self, c: Option<u32>) -> Self {
        self.color = c;
        self
    }

    pub fn color(&self) -> Option<u32> {
        self.color
    }

    pub fn next_block(&self) -> i32 {
        self.next_block
    }

    pub fn op_code(&self) -> &str {
        &self.op_code
    }

    pub fn param_types(&self) -> &[String] {
        // link with the tokenizer
        &self.param_types
    }

    pub fn parameters(&self) -> &[String] {
        &self.parameters
    }

    pub fn spec(&self) -> &str {
        &self.spec
    }

    pub fn sub_stack1(&self) -> i32 {
        self.sub_stack1
    }

    pub fn sub_stack2(&self) -> i32 {
        self.sub_stack2
    }

    pub fn block_type(&self) -> &str {
        &self.block_type
    }

    pub fn code(&self) -> &str {
        &self.code
    }

    pub fn spec2(&self) -> &str {
        &self.spec2
    }

    pub fn header_text(&self) -> &str {
        &self.header_text
    }

    pub fn to_json_collection(&self) -> Value {
        json!({
            "color": self.color(),
            "op_code": self.op_code(),
            "spec": self.spec(),
            "block_type": self.block_type(),
            "code": self.code(),
            "spec2": self.spec2(),
            "header_text": self.header_text(),
            "tokenized_spec": self.tokenized_spec,
            "parameters": self.parameters()
        })
    }

    pub fn full_json(&self) -> Value {
        json!({
            "id": self.id,
            "next_block": self.next_block(),
            "sub_stack1": self.sub_stack1(),
            "sub_stack2": self.sub_stack2(),
            "color": self.color(),
            "op_code": self.op_code(),
            "param_types": self.param_types(),
            "spec": self.spec(),
            "block_type": self.block_type(),
            "code": self.code(),
            "spec2": self.spec2(),
            "header_text": self.header_text(),
            "tokenized_spec": self.tokenized_spec,
            "parameters": self.parameters()
        })
    }
}

pub struct XmlViewBlockBean {
    color: Option<u32>,
    name: String,
    package: String,
    has_childs: bool,
}

impl XmlViewBlockBean {
    pub fn new() -> Self {
        Self {
            color: None,
            name: String::new(),
            package: String::new(),
            has_childs: true,
        }
    }

    pub fn set_name(mut self, v: &str) -> Self {
        self.name = v.to_string();
        self
    }

    pub fn set_color(mut self, c: u32) -> Self {
        self.color = Some(c);
        self
    }

    pub fn set_package(mut self, v: &str) -> Self {
        self.package = v.to_string();
        self
    }

    pub fn set_no_childs(mut self) -> Self {
        self.has_childs = false;
        self
    }

    pub fn build(self) -> BlockBean {
        let code;
        let block_type;

        let tag = if self.package.is_empty() {
            self.name.clone()
        } else {
            format!("{}.{}", self.package, self.name)
        };

        if self.has_childs {
            code = format!("<{tag}\n%s>\n%s\n</{tag}>", tag = tag);
            block_type = "e";
        } else {
            code = format!("<{tag} %s />", tag = tag);
            block_type = "c";
        }

        BlockBean::new()
            .set_op_code(&tag)
            .set_spec(&self.name)
            .set_code(&code)
            .set_type(block_type)
            .set_color(self.color)
    }
}

pub struct XmlAttrBlockBean {
    color: Option<u32>,
    pub attr: String,
    pub target_type: String,
    pub res: String,
    pub value_prefix: String,
    pub text_header: String,
}

impl XmlAttrBlockBean {
    pub fn new() -> Self {
        Self {
            color: None,
            attr: String::new(),
            target_type: String::from("asd"),
            res: String::from("android"),
            value_prefix: String::new(),
            text_header: String::new(),
        }
    }

    pub fn set_attr(mut self, v: &str) -> Self {
        self.attr = v.to_string();
        self
    }

    pub fn set_color(mut self, c: u32) -> Self {
        self.color = Some(c);
        self
    }

    pub fn set_header_text(mut self, text_header: &str) -> Self {
        self.text_header = text_header.to_string();
        self
    }

    pub fn set_value_prefix(mut self, prefix: &str) -> Self {
        self.value_prefix = prefix.to_string();
        self
    }

    pub fn set_target_type(mut self, target: &str) -> Self {
        self.target_type = target.to_string();
        self
    }

    pub fn build(self) -> BlockBean {
        let code = format!("%s:{}=\"{}%s\"", self.attr, self.value_prefix);

        BlockBean::new()
            .set_header_text(&self.text_header)
            .set_type(" ")
            .set_spec(&format!("%m.ResPrefix {} %{}", self.attr, self.target_type))
            .set_op_code(&self.attr)
            .set_code(&code)
            .push_parameter(self.res)
            .set_color(self.color)
    }
}
