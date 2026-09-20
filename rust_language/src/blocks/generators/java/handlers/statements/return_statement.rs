use crate::blocks::block_palette::BlockBean;
use crate::blocks::generators::java::coordinator::generation_coordinator::{
    DispatchType, GenerationCoordinator, StatementHandler,
};
use tree_sitter::Node;

pub struct ReturnStatement;

impl StatementHandler for ReturnStatement {
    fn get_block(kind: &str) -> BlockBean {
        let bean = BlockBean::new()
            .set_color(Some(0xFFE1A92A))
            .set_type("f")
            .set_op_code(kind);

        if kind == "return_something" {
            bean.set_header_text("Return statement")
                .set_code("return %s;")
                .set_spec("return %m.Object")
        } else {
            bean.set_code("return;")
                .set_spec("return")
        }
    }

    fn handle(
        node: &Node,
        generator_coordinator: &mut GenerationCoordinator,
        dispatch_type: DispatchType,
    ) {
        let return_expression = node.named_child(0);
        let kind = if return_expression.is_some() {
            "return_something"
        } else {
            "return_nothing"
        };

        let bean = Self::get_block(kind);
        let bean_id = generator_coordinator.push(bean, dispatch_type);

        if let Some(expr) = return_expression {
            generator_coordinator.dispatch_as_block_param(
                expr,
                bean_id,
                DispatchType::BlockParam("m.Object"),
            );
        }
    }
}

#[cfg(test)]
mod tests {
    use crate::blocks::generators::java::java_blocks_generator::JavaBlocksGenerator;

    #[test]
    fn test_return_statement() {
        let code = "return true;";
        let blocks = JavaBlocksGenerator::new(code.to_owned()).generate().unwrap();
        assert_eq!(blocks[0].op_code, "return_something");
        assert_eq!(blocks[1].op_code, "true");
    }
}
