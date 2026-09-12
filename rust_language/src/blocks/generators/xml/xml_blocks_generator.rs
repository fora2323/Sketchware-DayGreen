use crate::blocks::block_palette::{BlockBean, XmlAttrBlockBean, XmlViewBlockBean};
use crate::blocks::blocks_loader;
use roxmltree::{Document, Node};
use rustc_hash::FxHashMap;
use std::collections::HashSet;

pub struct XmlBlocksGenerator {
    xml_code: String,
}

struct GeneratorContext {
    counter: i32,
    blocks: Vec<BlockBean>,
    map: FxHashMap<String, BlockBean>,
    seen_namespaces: HashSet<String>,
}

impl GeneratorContext {
    fn new(map: FxHashMap<String, BlockBean>) -> Self {
        Self {
            counter: 10,
            blocks: Vec::new(),
            map,
            seen_namespaces: HashSet::new(),
        }
    }

    fn next_id(&mut self) -> i32 {
        let id = self.counter;
        self.counter += 1;
        id
    }

    fn add_block(&mut self, block: BlockBean) {
        self.blocks.push(block);
    }

    fn set_next_block(&mut self, from_id: i32, to_id: i32) {
        if let Some(b) = self.blocks.iter_mut().find(|b| b.id == from_id) {
            b.next_block = to_id;
        }
    }
}

impl XmlBlocksGenerator {
    pub fn new(xml_code: String) -> Self {
        Self { xml_code }
    }

    pub fn generate(&self) -> Result<Vec<BlockBean>, String> {
        let palettes = blocks_loader::load_lang("xml");
        let mut map = FxHashMap::default();

        for palette in palettes.iter() {
            for block in &palette.beans {
                map.insert(block.op_code().to_string(), block.clone());
            }
        }

        let mut ctx = GeneratorContext::new(map);

        let doc = match Document::parse(self.xml_code.as_str()) {
            Ok(doc) => doc,
            Err(e) => {
                let msg = format_panic_like(
                    "src/blocks/xml_blocks_generator.rs",
                    &format!("XML syntax error: {}", e),
                );
                return Err(msg);
            }
        };

        let root_elements: Vec<Node> = doc.root().children().filter(|n| n.is_element()).collect();

        let mut prev_root_id = -1;
        for node in root_elements {
            let id = self.process_node(&mut ctx, node);
            if prev_root_id != -1 {
                ctx.set_next_block(prev_root_id, id);
            }
            prev_root_id = id;
        }

        ctx.blocks.sort_by_key(|b| b.id);
        Ok(ctx.blocks)
    }

    fn process_node(&self, ctx: &mut GeneratorContext, node: Node) -> i32 {
        let default_color: u32 = 0xFFC8A030;
        let view_name = node.tag_name().name();

        let mut view_block = if let Some(bean) = ctx.map.get(view_name) {
            bean.clone()
        } else {
            XmlViewBlockBean::new()
                .set_name(view_name)
                .set_color(default_color)
                .build()
        };

        let block_id = ctx.next_id();
        view_block.id = block_id;

        let mut first_ns_id: i32 = -1;
        let mut prev_ns_id: i32 = -1;

        for ns in node.namespaces() {
            let key = format!("{}={}", ns.name().unwrap_or(""), ns.uri());
            if ctx.seen_namespaces.insert(key.clone()) {
                let ns_op = match ns.name().unwrap_or("") {
                    "android" => "android_namespace",
                    "app" => "app_namespace",
                    "tools" => "tools_namespace",
                    _ => continue,
                };

                if let Some(ns_block_template) = ctx.map.get(ns_op) {
                    let mut ns_block = ns_block_template.clone();
                    let ns_id = ctx.next_id();
                    ns_block.id = ns_id;

                    if first_ns_id == -1 {
                        first_ns_id = ns_id;
                    }
                    if prev_ns_id != -1 {
                        ctx.set_next_block(prev_ns_id, ns_id);
                    }

                    prev_ns_id = ns_id;
                    ctx.add_block(ns_block);
                }
            }
        }

        let mut first_attr_id: i32 = -1;
        let mut prev_attr_id: i32 = -1;

        let mut ns_map: FxHashMap<&str, &str> = FxHashMap::default();
        for ns in node.namespaces() {
            if let Some(name) = ns.name() {
                ns_map.insert(ns.uri(), name);
            }
        }

        for attr in node.attributes() {
            let mut attr_block = if let Some(bean) = ctx.map.get(attr.name()) {
                bean.clone()
            } else {
                let block_type = if matches!(attr.name(), "true" | "false") {
                    "b"
                } else {
                    "asd"
                };

                XmlAttrBlockBean::new()
                    .set_attr(attr.name())
                    .set_color(default_color)
                    .set_target_type(block_type)
                    .build()
            };

            let attr_id = ctx.next_id();
            attr_block.id = attr_id;

            attr_block.parameters.clear();
            let uri = attr.namespace().unwrap_or("");
            let param1 = ns_map.get(uri).unwrap_or(&"");
            let param2 = if attr.name() == "id" {
                attr.value().replace("@+id/", "")
            } else {
                attr.value().to_string()
            };

            if !param1.is_empty() {
                attr_block.parameters.push(param1.to_string());
            }
            attr_block.parameters.push(param2);

            if first_attr_id == -1 {
                first_attr_id = attr_id;
                if prev_ns_id != -1 {
                    ctx.set_next_block(prev_ns_id, attr_id);
                }
            }

            if prev_attr_id != -1 {
                ctx.set_next_block(prev_attr_id, attr_id);
            }

            prev_attr_id = attr_id;
            ctx.add_block(attr_block);
        }

        if first_ns_id != -1 {
            view_block.sub_stack1 = first_ns_id;
        } else {
            view_block.sub_stack1 = first_attr_id;
        }

        let mut first_child_id: i32 = -1;
        let mut prev_child_id: i32 = -1;

        for child in node.children().filter(|n| n.is_element()) {
            let child_id = self.process_node(ctx, child);

            if first_child_id == -1 {
                first_child_id = child_id;
            }

            if prev_child_id != -1 {
                ctx.set_next_block(prev_child_id, child_id);
            }
            prev_child_id = child_id;
        }

        view_block.sub_stack2 = first_child_id;

        ctx.add_block(view_block);
        block_id
    }
}

pub fn format_panic_like(location: &str, message: &str) -> String {
    format!(
        " - Error: {}\n\n - Thread 'Code2Blocks.XmlLayouts2Blocks.main' panicked at {}",
        message, location
    )
}
