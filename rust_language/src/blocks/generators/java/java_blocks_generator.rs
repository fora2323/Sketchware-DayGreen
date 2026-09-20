use crate::blocks::block_palette::BlockBean;
use crate::blocks::generators::java::coordinator::generation_coordinator::{
    DispatchType, GenerationCoordinator,
};
use tree_sitter::Parser;

#[derive(Debug)]
pub struct JavaBlocksGenerator {
    code: String,
}

impl JavaBlocksGenerator {
    pub fn new(code: String) -> Self {
        Self { code }
    }

    pub fn generate(self) -> Result<Vec<BlockBean>, String> {
        let mut parser = Parser::new();
        parser
            .set_language(&tree_sitter_java::language())
            .map_err(|e| format!("Failed to set language: {:?}", e))?;

        let tree = parser
            .parse(&self.code, None)
            .ok_or_else(|| "Failed to parse code".to_string())?;

        let root = tree.root_node();
        let mut ctx = GenerationCoordinator::new(self.code);

        let children: Vec<_> = root.children(&mut root.walk()).collect();
        let total = children.len();
        for (i, child) in children.into_iter().enumerate() {
            let dispatch_type = if i == total - 1 {
                DispatchType::LastChild
            } else {
                DispatchType::NextBlock
            };
            ctx.dispatch_with_type(&child, dispatch_type);
        }

        Ok(ctx.build())
    }
}
