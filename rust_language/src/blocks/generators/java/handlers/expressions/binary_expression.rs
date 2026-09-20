use crate::blocks::block_palette::BlockBean;
use crate::blocks::generators::java::coordinator::generation_coordinator::{
    DispatchType, GenerationCoordinator, StatementHandler,
};
use tree_sitter::Node;

pub struct BinaryExpressionHandler;

impl StatementHandler for BinaryExpressionHandler {
    fn get_block(kind: &str) -> BlockBean {
        let (spec, block_type) = match kind {
            ">" | "<" | "==" | "!=" | ">=" | "<=" | "&&" | "||" | "instanceof" => {
                let s = match kind {
                    "&&" => "%b and %b".to_string(),
                    "||" => "%b or %b".to_string(),
                    "instanceof" => "%m.Object instanceof %m.Type".to_string(),
                    _ => "%d %s %d".replace("%s", kind),
                };
                (s, "b")
            }
            "&" | "|" | "^" | "<<" | ">>" | ">>>" => ("%d %s %d".replace("%s", kind), "d"),
            _ => ("%d %s %d".replace("%s", kind), "d"),
        };

        let actual_op = match kind {
            "==" => "=",
            _ => kind,
        };

        BlockBean::new()
            .set_op_code(actual_op)
            .set_type(block_type)
            .set_code(&format!("%s {} %s", kind))
            .set_spec(&spec)
            .set_color(Some(0xFF5CB722))
    }

    fn handle(
        node: &Node,
        generator_coordinator: &mut GenerationCoordinator,
        dispatch_type: DispatchType,
    ) {
        let op_code = generator_coordinator
            .get_code(&node.child_by_field_name("operator").unwrap())
            .to_string();

        let bean = Self::get_block(&op_code);
        let block_type = bean.block_type().to_owned();

        let expected_type = if block_type == "b" {
            if op_code == "&&" || op_code == "||" {
                "b"
            } else if op_code == "instanceof" {
                "m.Object"
            } else {
                "d"
            }
        } else {
            "d"
        };

        let bean_id = generator_coordinator.push(bean, dispatch_type);

        if let Some(left_node) = node.child_by_field_name("left") {
            generator_coordinator.dispatch_as_block_param(
                left_node,
                bean_id,
                DispatchType::BlockParam(expected_type),
            );
        }

        if let Some(right_node) = node.child_by_field_name("right") {
            let right_expected = if op_code == "instanceof" {
                "m.Type"
            } else {
                expected_type
            };
            generator_coordinator.dispatch_as_block_param(
                right_node,
                bean_id,
                DispatchType::BlockParam(right_expected),
            );
        }
    }
}
