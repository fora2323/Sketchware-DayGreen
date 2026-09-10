use crate::blocks::block_palette::BlockBean;
use crate::blocks::generators::java::coordinator::generation_coordinator::{
    DispatchType, GenerationCoordinator, StatementHandler,
};
use tree_sitter::Node;

pub struct InstanceOfExpressionHandler;

impl StatementHandler for InstanceOfExpressionHandler {
    fn get_block(kind: &str) -> BlockBean {
        BlockBean::new()
            .set_op_code(kind)
            .set_type("b")
            .set_code("%s instanceof %s")
            .set_spec("is %m.Object instance of %m.type")
            .set_color(Some(0xFF5CB722))
    }

    fn handle(
        node: &Node,
        generator_coordinator: &mut GenerationCoordinator,
        dispatch_type: DispatchType,
    ) {
        let bean = Self::get_block(node.kind());
        let bean_id = generator_coordinator.push(bean, dispatch_type);

        let left_node = node.child_by_field_name("left").unwrap();

        generator_coordinator.dispatch_as_block_param(
            left_node,
            bean_id,
            DispatchType::BlockParam("m.Object"),
        );

        if let Some(right) = node.child_by_field_name("right") {
            let right_code = generator_coordinator.get_code(&right).to_string();
            let name_code = node
                .child_by_field_name("name")
                .map(|name| generator_coordinator.get_code(&name).to_string());

            generator_coordinator.borrow_mut_block(bean_id, |bean| {
                let mut full_right = right_code;
                if let Some(name_str) = name_code {
                    full_right.push(' ');
                    full_right.push_str(&name_str);
                }
                bean.parameters.push(full_right);
            });
        } else if let Some(record_pattern_node) = node.child_by_field_name("pattern") {
            generator_coordinator.dispatch_as_block_param(
                record_pattern_node,
                bean_id,
                DispatchType::BlockParam("m.type"),
            );
        }
    }
}

