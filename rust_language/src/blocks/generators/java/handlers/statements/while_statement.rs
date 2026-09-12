use crate::blocks::block_palette::BlockBean;
use crate::blocks::generators::java::coordinator::generation_coordinator::{
    DispatchType, GenerationCoordinator, StatementHandler,
};
use tree_sitter::Node;

pub struct WhileStatement;

impl StatementHandler for WhileStatement {
    fn get_block(kind: &str) -> BlockBean {
        if kind == "forever" {
            BlockBean::new()
                .set_op_code("forever")
                .set_type("c")
                .set_code("while (true) {\n%s\n}")
                .set_spec("forever")
                .set_color(Some(0xFFE1A92A))
        } else {
            BlockBean::new()
                .set_op_code("while")
                .set_type("c")
                .set_code("while (%s) {\n%s\n}")
                .set_spec("while %b")
                .set_color(Some(0xFFE1A92A))
        }
    }

    fn handle(
        node: &Node,
        generator_coordinator: &mut GenerationCoordinator,
        dispatch_type: DispatchType,
    ) {
        let condition_str = if let Some(cond) = node.child_by_field_name("condition") {
            generator_coordinator.get_code(&cond).trim().to_string()
        } else {
            String::new()
        };

        let is_forever = condition_str == "(true)" || condition_str == "true";
        let kind = if is_forever { "forever" } else { "while" };

        let bean = Self::get_block(kind);
        let bean_id = generator_coordinator.push(bean, dispatch_type);

        if !is_forever {
            if let Some(condition_node) = node.child_by_field_name("condition") {
                let inner_node = condition_node.child(1).unwrap_or(condition_node);
                generator_coordinator.dispatch_as_block_param(
                    inner_node,
                    bean_id,
                    DispatchType::BlockParam("b"),
                );
            }
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

#[cfg(test)]
mod tests {
    use crate::blocks::generators::java::java_blocks_generator::JavaBlocksGenerator;

    #[test]
    fn test_while_statement() {
        let code = "while (true) { // hi \n }";
        let blocks = JavaBlocksGenerator::new(code.to_owned()).generate().unwrap();
        assert_eq!(blocks[0].op_code, "while_statement");
        assert_eq!(blocks[1].op_code, "true");
        assert_eq!(blocks[2].op_code, "line_comment");
    }
}
