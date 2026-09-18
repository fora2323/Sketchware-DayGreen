use crate::blocks::block_palette::BlockBean;
use crate::blocks::generators::java::coordinator::generation_coordinator::{
    DispatchType, GenerationCoordinator, StatementHandler,
};
use tree_sitter::Node;

pub struct ForStatement;

impl StatementHandler for ForStatement {
    fn get_block(_: &str) -> BlockBean {
        BlockBean::new()
            .set_op_code("repeat")
            .set_type("c")
            .set_spec("repeat %d")
            .set_code("for (int _repeat%s = 0; _repeat%s < (int)(%s); _repeat%s++) {\n%s\n}")
            .set_color(Some(0xFFE1A92A))
    }

    fn handle(
        node: &Node,
        generator_coordinator: &mut GenerationCoordinator,
        dispatch_type: DispatchType,
    ) {
        let bean = Self::get_block("repeat");
        let bean_id = generator_coordinator.push(bean, dispatch_type);

        // Extract count from condition (e.g. i < count)
        if let Some(cond) = node.child_by_field_name("condition") {
            if cond.kind() == "binary_expression" {
                if let Some(right) = cond.child_by_field_name("right") {
                    let uncast_node = if right.kind() == "cast_expression" {
                        right.child_by_field_name("value").unwrap_or(right)
                    } else if right.kind() == "parenthesized_expression" {
                        right.child(1).unwrap_or(right)
                    } else {
                        right
                    };

                    generator_coordinator.dispatch_as_block_param(
                        uncast_node,
                        bean_id,
                        DispatchType::BlockParam("d"),
                    );
                } else {
                    generator_coordinator.borrow_mut_block(bean_id, |b| {
                        b.parameters.push("10".to_string());
                    });
                }
            } else {
                generator_coordinator.borrow_mut_block(bean_id, |b| {
                    b.parameters.push("10".to_string());
                });
            }
        } else {
            generator_coordinator.borrow_mut_block(bean_id, |b| {
                b.parameters.push("10".to_string());
            });
        }

        if let Some(body) = node.child_by_field_name("body") {
            process_body(body, generator_coordinator, bean_id);
        }
    }
}

fn process_body(node: Node, generator_coordinator: &mut GenerationCoordinator, bean_id: usize) {
    let child_count = node.child_count();

    let (start, end) = if node.kind() == "block" {
        (1, child_count - 1)
    } else {
        (0, child_count)
    };

    let mut dispatched = false;
    let total = end - start;

    for (pos, i) in (start..end).enumerate() {
        if let Some(grandchild) = node.child(i) {
            if !dispatched {
                let next_id = generator_coordinator.get_next_id();
                generator_coordinator.borrow_mut_block(bean_id, |bean| {
                    bean.sub_stack1 = next_id;
                });
                dispatched = true;
            }

            generator_coordinator.dispatch(&grandchild, pos, total);
        }
    }
}
