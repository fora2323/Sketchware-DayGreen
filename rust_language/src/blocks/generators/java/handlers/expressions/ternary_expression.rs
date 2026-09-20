use crate::blocks::block_palette::BlockBean;
use crate::blocks::generators::java::coordinator::generation_coordinator::{
    DispatchType, GenerationCoordinator, StatementHandler,
};
use tree_sitter::Node;

pub struct TernaryExpressionHandler;

impl StatementHandler for TernaryExpressionHandler {
    fn get_block(kind: &str) -> BlockBean {
        BlockBean::new()
            .set_op_code(kind)
            .set_type("s")
            .set_code("%s ? %s : %s")
            .set_spec("if %b then %s else %s")
            .set_color(Some(0xFF5CB722))
    }

    fn handle(
        node: &Node,
        generator_coordinator: &mut GenerationCoordinator,
        dispatch_type: DispatchType,
    ) {
        let condition = node.child_by_field_name("condition").unwrap();
        let consequence = node.child_by_field_name("consequence").unwrap();
        let alternative = node.child_by_field_name("alternative").unwrap();

        let bean = Self::get_block(node.kind());
        let bean_id = generator_coordinator.push(bean, dispatch_type);

        generator_coordinator.dispatch_as_block_param(
            condition,
            bean_id,
            DispatchType::BlockParam("b"),
        );
        generator_coordinator.dispatch_as_block_param(
            consequence,
            bean_id,
            DispatchType::BlockParam("s"),
        );
        generator_coordinator.dispatch_as_block_param(
            alternative,
            bean_id,
            DispatchType::BlockParam("s"),
        );
    }
}
