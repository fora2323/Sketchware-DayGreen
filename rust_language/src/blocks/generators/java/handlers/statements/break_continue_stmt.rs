use crate::blocks::block_palette::BlockBean;
use crate::blocks::generators::java::coordinator::generation_coordinator::{
    DispatchType, GenerationCoordinator, StatementHandler,
};
use tree_sitter::Node;

pub struct BreakContinueStatement;

impl StatementHandler for BreakContinueStatement {
    fn get_block(kind: &str) -> BlockBean {
        let (op_code, spec, code) = if kind == "break_statement" {
            ("break", "break", "break;")
        } else {
            ("continue_statement", "continue", "continue;")
        };

        BlockBean::new()
            .set_op_code(op_code)
            .set_type("f")
            .set_spec(spec)
            .set_code(code)
            .set_color(Some(0xFFE1A92A))
    }

    fn handle(
        node: &Node,
        generator_coordinator: &mut GenerationCoordinator,
        dispatch_type: DispatchType,
    ) {
        let bean = Self::get_block(node.kind());
        generator_coordinator.push(bean, dispatch_type);
    }
}

#[cfg(test)]
mod tests {
    use crate::blocks::generators::java::java_blocks_generator::JavaBlocksGenerator;

    #[test]
    fn test_break_continue() {
        let code = "break; continue;";
        let blocks = JavaBlocksGenerator::new(code.to_owned()).generate().unwrap();
        assert_eq!(blocks[0].op_code, "break");
        assert_eq!(blocks[1].op_code, "continue_statement");
    }
}
