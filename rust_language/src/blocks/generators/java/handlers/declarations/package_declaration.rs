use crate::blocks::block_palette::BlockBean;
use crate::blocks::generators::java::coordinator::generation_coordinator::{
    DispatchType, GenerationCoordinator, StatementHandler,
};
use tree_sitter::Node;

pub struct PackageDeclarationStatement;

impl StatementHandler for PackageDeclarationStatement {
    fn get_block(kind: &str) -> BlockBean {
        BlockBean::new()
            .set_header_text("imports & package")
            .set_op_code(kind)
            .set_type(" ")
            .set_code("package %s;")
            .set_spec("package %asd")
            .set_color(Some(0xFFE1A92A))
    }

    fn handle(
        node: &Node,
        generator_coordinator: &mut GenerationCoordinator,
        dispatch_type: DispatchType,
    ) {
        let mut cursor = node.walk();
        for child in node.named_children(&mut cursor) {
            if matches!(child.kind(), "annotation" | "marker_annotation") {
                generator_coordinator.dispatch_with_type(&child, DispatchType::NextBlock);
            }
        }

        let full = generator_coordinator.get_code(node).to_string();
        if let Some(start) = full.find("package ") {
            let package_name = full[start + 8..].trim().trim_end_matches(';').trim();
            let mut bean = Self::get_block(node.kind());
            bean.parameters.push(package_name.to_string());
            generator_coordinator.push(bean, dispatch_type);
        }
    }
}

#[cfg(test)]
mod tests {
    use crate::blocks::generators::java::java_blocks_generator::JavaBlocksGenerator;

    #[test]
    fn test_package_declaration() {
        let code = "@Anno package com.example;";
        let blocks = JavaBlocksGenerator::new(code.to_owned()).generate().unwrap();
        assert_eq!(blocks[0].op_code, "@Anno");
        assert_eq!(blocks[1].op_code, "package_declaration");
        assert_eq!(blocks[1].parameters[0], "com.example");
    }
}
