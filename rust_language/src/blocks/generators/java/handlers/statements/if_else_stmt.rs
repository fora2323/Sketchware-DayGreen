use crate::blocks::block_palette::BlockBean;
use crate::blocks::generators::java::coordinator::generation_coordinator::{
    DispatchType, GenerationCoordinator, StatementHandler,
};
use tree_sitter::Node;

pub struct IfElseStatement;

impl StatementHandler for IfElseStatement {
    fn get_block(kind: &str) -> BlockBean {
        let mut bean = BlockBean::new()
            .set_op_code(kind)
            .set_type(if kind == "if" { "c" } else { "e" })
            .set_color(Some(0xFFE1A92A));

        if kind == "if" {
            bean = bean.set_code("if (%s) {\n%s\n}")
                .set_spec("if %b then");
        } else {
            bean = bean.set_code("if (%s) {\n%s\n} else {\n%s\n} ")
                .set_spec("if %b then")
                .set_spec2("else");
        }
        bean
    }

    fn handle(
        node: &Node,
        generator_coordinator: &mut GenerationCoordinator,
        dispatch_type: DispatchType,
    ) {
        let kind = if node.child_by_field_name("alternative").is_some() {
            "ifElse"
        } else {
            "if"
        };

        let bean = Self::get_block(kind);
        let bean_id = generator_coordinator.push(bean, dispatch_type);

        if let Some(condition_node) = node.child_by_field_name("condition") {
            generator_coordinator.dispatch_as_block_param(
                condition_node.child(1).unwrap(),
                bean_id,
                DispatchType::BlockParam("b"),
            )
        }

        if let Some(consequence_node) = node.child_by_field_name("consequence") {
            process_body(consequence_node, generator_coordinator, bean_id, true);
        }

        if let Some(alternative_node) = node.child_by_field_name("alternative") {
            if alternative_node.kind() == "if_statement" {
                let next_id = generator_coordinator.get_next_id();
                generator_coordinator.borrow_mut_block(bean_id, |bean| {
                    bean.sub_stack2 = next_id;
                });
                generator_coordinator.dispatch_with_type(&alternative_node, DispatchType::LastChild);
            } else {
                process_body(alternative_node, generator_coordinator, bean_id, false);
            }
        }
    }
}

fn process_body(
    node: Node,
    generator_coordinator: &mut GenerationCoordinator,
    bean_id: usize,
    consequence_node: bool,
) {
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
                    if consequence_node {
                        bean.sub_stack1 = next_id;
                    } else {
                        bean.sub_stack2 = next_id;
                    }
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
    fn test_if_else_statement() {
        let code = r#"
            if (true) {
                // hi
            } else {
                // bye
            }
        "#;

        let blocks = JavaBlocksGenerator::new(code.to_owned())
            .generate()
            .unwrap();

        assert_eq!(blocks[0].op_code, "ifElse");
        assert_eq!(blocks[1].op_code, "true");
        assert_eq!(blocks[2].op_code, "line_comment");
        assert_eq!(blocks[2].parameters[0], " hi");
        assert_eq!(blocks[3].op_code, "line_comment");
        assert_eq!(blocks[3].parameters[0], " bye");
    }
}
