use crate::blocks::block_palette::BlockBean;
use crate::blocks::generators::java::coordinator::generation_coordinator::{
    DispatchType, GenerationCoordinator, StatementHandler,
};
use tree_sitter::Node;

pub struct UpdateExpressionHandler;

impl StatementHandler for UpdateExpressionHandler {
    fn get_block(kind: &str) -> BlockBean {
        if kind == "decreaseInt" {
            BlockBean::new()
                .set_op_code("decreaseInt")
                .set_type(" ")
                .set_spec("decrease %m.varInt")
                .set_code("%s--;")
                .set_color(Some(0xFFEE7D16))
        } else {
            BlockBean::new()
                .set_op_code("increaseInt")
                .set_type(" ")
                .set_spec("increase %m.varInt")
                .set_code("%s++;")
                .set_color(Some(0xFFEE7D16))
        }
    }

    fn handle(
        node: &Node,
        generator_coordinator: &mut GenerationCoordinator,
        dispatch_type: DispatchType,
    ) {
        let first = node.child(0).unwrap();
        let second = node.child(1).unwrap();

        let (is_decrement, target_node) = if matches!(first.kind(), "++" | "--") {
            (first.kind() == "--", second)
        } else {
            (second.kind() == "--", first)
        };

        let op_code = if is_decrement { "decreaseInt" } else { "increaseInt" };
        let mut bean = Self::get_block(op_code);
        let target_code = generator_coordinator.get_code(&target_node).to_string();
        bean.parameters.push(target_code);

        generator_coordinator.push(bean, dispatch_type);
    }
}
