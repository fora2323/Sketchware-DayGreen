use crate::blocks::block_palette::BlockBean;
use crate::blocks::generators::java::coordinator::generation_coordinator::{
    DispatchType, GenerationCoordinator, StatementHandler,
};
use tree_sitter::Node;

pub struct UnaryExpressionHandler;

impl StatementHandler for UnaryExpressionHandler {
    fn get_block(kind: &str) -> BlockBean {
        let bean = BlockBean::new()
            .set_op_code(kind)
            .set_color(Some(0xFF5CB722));

        match kind {
            "!" => bean.set_op_code("not").set_type("b").set_code("!%s").set_spec("not %b"),
            "-" => bean.set_type("d").set_code("-%s").set_spec("minus %d"),
            "~" => bean.set_type("d").set_code("~%s").set_spec("bitwise not %d"),
            _ => bean.set_type("d")
                .set_code(&format!("{}%s", kind))
                .set_spec(&format!("{} %d", kind)),
        }
    }

    fn handle(
        node: &Node,
        generator_coordinator: &mut GenerationCoordinator,
        dispatch_type: DispatchType,
    ) {
        let operator_node = node.child_by_field_name("operator").unwrap();
        let operand_node = node.child_by_field_name("operand").unwrap();

        let op_kind = operator_node.kind();
        let bean = Self::get_block(op_kind);
        let expected_type = if bean.block_type() == "b" { "b" } else { "d" };

        let bean_id = generator_coordinator.push(bean, dispatch_type);

        generator_coordinator.dispatch_as_block_param(
            operand_node,
            bean_id,
            DispatchType::BlockParam(expected_type),
        );
    }
}
