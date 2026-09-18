use crate::blocks::block_palette::BlockBean;
use crate::blocks::generators::java::coordinator::generation_coordinator::{
    DispatchType, GenerationCoordinator, StatementHandler,
};
use tree_sitter::Node;

pub struct LocalVariableDeclarationStatement;

impl StatementHandler for LocalVariableDeclarationStatement {
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
        let type_node = node.child_by_field_name("type");
        let type_str = type_node
            .map(|t| generator_coordinator.get_code(&t).trim().to_string())
            .unwrap_or_default();

        let val_type = match type_str.as_str() {
            "boolean" | "Boolean" => "boolean",
            "int" | "double" | "float" | "long" | "Integer" | "Double" | "Float" | "Long" => "number",
            _ => "string",
        };

        let mut cursor = node.walk();
        for child in node.children(&mut cursor) {
            if child.kind() == "variable_declarator" {
                let name_node = child.child_by_field_name("name");
                let value_node = child.child_by_field_name("value");

                if let Some(name) = name_node {
                    let var_name = generator_coordinator.get_code(&name).trim().to_string();

                    if let Some(value) = value_node {
                        let bean = Self::get_block(val_type);
                        let bean_id = generator_coordinator.push(bean, dispatch_type);

                        generator_coordinator.borrow_mut_block(bean_id, |b| {
                            b.parameters.push(var_name);
                        });

                        let expected_param = match val_type {
                            "boolean" => "b",
                            "number" => "d",
                            _ => "s",
                        };

                        generator_coordinator.dispatch_as_block_param(
                            value,
                            bean_id,
                            DispatchType::BlockParam(expected_param),
                        );
                        return;
                    }
                }
            }
        }

        // Fallback for uninitialized declaration
        let code = generator_coordinator.get_code(node).to_string();
        let mut bean = BlockBean::new()
            .set_op_code("addSourceDirectly")
            .set_type(" ")
            .set_code("%s")
            .set_spec("add source directly %s")
            .set_color(Some(0xFF5CB722));
        bean.parameters.push(code);
        generator_coordinator.push(bean, dispatch_type);
    }
}
