use crate::blocks::block_palette::BlockBean;
use crate::blocks::generators::java::coordinator::generation_coordinator::{
    DispatchType, GenerationCoordinator, StatementHandler,
};
use tree_sitter::Node;

pub struct CastStatementHandler;

impl StatementHandler for CastStatementHandler {
    fn get_block(kind: &str) -> BlockBean {
        BlockBean::new()
            .set_op_code(kind)
            .set_type("v.Object")
            .set_code("(%s) %s")
            .set_spec("cast %m.Object to %m.type")
            .set_color(Some(0xFF5CB722))
    }

    fn handle(
        node: &Node,
        generator_coordinator: &mut GenerationCoordinator,
        dispatch_type: DispatchType,
    ) {
        let bean = Self::get_block(node.kind());
        let bean_id = generator_coordinator.push(bean, dispatch_type);

        let type_node = node.child_by_field_name("type").unwrap();
        let value_node = node.child_by_field_name("value").unwrap();

        let type_code = generator_coordinator.get_code(&type_node).to_string();
        generator_coordinator.borrow_mut_block(bean_id, |bean| {
            bean.parameters.push(type_code);
        });

        generator_coordinator.dispatch_as_block_param(
            value_node,
            bean_id,
            DispatchType::BlockParam("m.Object"),
        );
    }
}