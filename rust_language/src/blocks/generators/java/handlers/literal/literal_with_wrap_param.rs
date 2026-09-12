use crate::blocks::block_palette::BlockBean;
use crate::blocks::generators::java::coordinator::generation_coordinator::{
    DispatchType, GenerationCoordinator, StatementHandler,
};
use tree_sitter::Node;

pub struct LiteralWithWrapParamStatement;

impl StatementHandler for LiteralWithWrapParamStatement {
    fn get_block(kind: &str) -> BlockBean {
        let (block_type, spec_prefix) = match kind {
            "decimal_integer_literal"
            | "hex_integer_literal"
            | "octal_integer_literal"
            | "binary_integer_literal" => ("d", "integer"),
            "decimal_floating_point_literal" | "hex_floating_point_literal" => ("d", "number"),
            "string_literal" => ("s", "string"),
            "character_literal" => ("s", "char"),
            _ => ("v.Object", "value"),
        };

        BlockBean::new()
            .set_op_code(kind)
            .set_type(block_type)
            .set_code("%s")
            .set_spec(&format!("{} %asd", spec_prefix))
            .set_color(Some(0xFF5CB722))
    }

    fn handle(
        node: &Node,
        generator_coordinator: &mut GenerationCoordinator,
        dispatch_type: DispatchType,
    ) {
        let mut bean = Self::get_block(node.kind());
        let code = generator_coordinator.get_code(node).to_string();
        bean.parameters.push(code);

        generator_coordinator.push(bean, dispatch_type);
    }
}
