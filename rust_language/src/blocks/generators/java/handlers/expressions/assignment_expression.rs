use crate::blocks::block_palette::BlockBean;
use crate::blocks::generators::java::coordinator::generation_coordinator::{
    DispatchType, GenerationCoordinator, StatementHandler,
};
use tree_sitter::Node;

pub struct AssignmentExpressionHandler;

impl StatementHandler for AssignmentExpressionHandler {
    fn get_block(kind: &str) -> BlockBean {
        match kind {
            "boolean" => BlockBean::new()
                .set_op_code("setVarBoolean")
                .set_type(" ")
                .set_spec("set %m.varBool to %b")
                .set_code("%s = %s;")
                .set_color(Some(0xFFEE7D16)),
            "number" => BlockBean::new()
                .set_op_code("setVarInt")
                .set_type(" ")
                .set_spec("set %m.varInt to %d")
                .set_code("%s = %s;")
                .set_color(Some(0xFFEE7D16)),
            _ => BlockBean::new()
                .set_op_code("setVarString")
                .set_type(" ")
                .set_spec("set %m.varStr to %s")
                .set_code("%s = %s;")
                .set_color(Some(0xFFEE7D16)),
        }
    }

    fn handle(
        node: &Node,
        generator_coordinator: &mut GenerationCoordinator,
        dispatch_type: DispatchType,
    ) {
        let left_node = match node.child_by_field_name("left") {
            Some(l) => l,
            None => return,
        };
        let right_node = match node.child_by_field_name("right") {
            Some(r) => r,
            None => return,
        };

        let left_name = generator_coordinator.get_code(&left_node).trim().to_string();
        let right_kind = right_node.kind();

        let val_type = match right_kind {
            "true" | "false" => "boolean",
            "decimal_integer_literal" | "decimal_floating_point_literal" | "hex_integer_literal" => "number",
            "string_literal" => "string",
            "binary_expression" => {
                let op = right_node.child_by_field_name("operator")
                    .map(|o| generator_coordinator.get_code(&o).to_string())
                    .unwrap_or_default();
                match op.as_str() {
                    "==" | "!=" | "<" | ">" | "<=" | ">=" | "&&" | "||" => "boolean",
                    _ => "number",
                }
            }
            "unary_expression" => {
                let op = right_node.child_by_field_name("operator")
                    .map(|o| generator_coordinator.get_code(&o).to_string())
                    .unwrap_or_default();
                if op == "!" { "boolean" } else { "number" }
            }
            _ => "string",
        };

        let bean = Self::get_block(val_type);
        let bean_id = generator_coordinator.push(bean, dispatch_type);

        generator_coordinator.borrow_mut_block(bean_id, |b| {
            b.parameters.push(left_name);
        });

        let expected_param = match val_type {
            "boolean" => "b",
            "number" => "d",
            _ => "s",
        };

        generator_coordinator.dispatch_as_block_param(
            right_node,
            bean_id,
            DispatchType::BlockParam(expected_param),
        );
    }
}
