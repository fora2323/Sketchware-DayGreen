use crate::blocks::block_palette::BlockBean;
use crate::blocks::generators::java::coordinator::generation_coordinator::{
    DispatchType, GenerationCoordinator, StatementHandler,
};
use tree_sitter::Node;

pub struct ImportDeclarationStatement;

impl StatementHandler for ImportDeclarationStatement {
    fn get_block(kind: &str) -> BlockBean {
        let (spec, code) = if kind == "static_import_declaration" {
            ("static import %asd", "import static %s;")
        } else {
            ("import %asd", "import %s;")
        };

        BlockBean::new()
            .set_header_text("imports & package")
            .set_op_code(kind)
            .set_type(" ")
            .set_code(code)
            .set_spec(spec)
            .set_color(Some(0xFFE1A92A))
    }

    fn handle(
        node: &Node,
        generator_coordinator: &mut GenerationCoordinator,
        dispatch_type: DispatchType,
    ) {
        let full_code = generator_coordinator.get_code(node).to_string();
        let (kind, start_offset) = if full_code.starts_with("import static") {
            ("static_import_declaration", 13)
        } else {
            ("import_declaration", 7)
        };

        let param = full_code[start_offset..full_code.len() - 1].trim().to_string();
        let mut bean = Self::get_block(kind);
        bean.parameters.push(param);

        generator_coordinator.push(bean, dispatch_type);
    }
}

#[cfg(test)]
mod tests {
    use crate::blocks::generators::java::java_blocks_generator::JavaBlocksGenerator;

    #[test]
    fn test_import() {
        let code = "import java.util.List; import static java.lang.Math.PI;";
        let blocks = JavaBlocksGenerator::new(code.to_owned()).generate().unwrap();
        assert_eq!(blocks[0].op_code, "import_declaration");
        assert_eq!(blocks[0].parameters[0], "java.util.List");
        assert_eq!(blocks[1].op_code, "static_import_declaration");
        assert_eq!(blocks[1].parameters[0], "java.lang.Math.PI");
    }
}
