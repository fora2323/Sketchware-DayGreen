use crate::blocks::block_palette::BlockBean;
use crate::blocks::generators::java::coordinator::generation_coordinator::{
    DispatchType, GenerationCoordinator, StatementHandler,
};
use tree_sitter::Node;

pub struct CommentStatement;

impl StatementHandler for CommentStatement {
    fn get_block(kind: &str) -> BlockBean {
        let mut bean = BlockBean::new()
            .set_op_code(kind)
            .set_type(" ")
            .set_color(Some(0xFFE1A92A));

        if kind == "line_comment" {
            bean = bean.set_header_text("comments")
                .set_code("// %s")
                .set_spec("comment %asd");
        } else {
            bean = bean.set_code("/* %s */").set_spec("block comment %asd");
        }
        bean
    }

    fn handle(
        node: &Node,
        generator_coordinator: &mut GenerationCoordinator,
        dispatch_type: DispatchType,
    ) {
        let op_code = node.kind();
        let mut bean = Self::get_block(op_code);

        let param = if op_code == "line_comment" {
            generator_coordinator
                .get_code_with_range(node, 2..)
                .to_string()
        } else {
            let full_comment_text = generator_coordinator.get_code(node);
            if full_comment_text.len() >= 4 {
                full_comment_text[2..full_comment_text.len() - 2].to_string()
            } else {
                "".to_string()
            }
        };
        bean.parameters.push(param);
        generator_coordinator.push(bean, dispatch_type);
    }
}

#[cfg(test)]
mod tests {
    use crate::blocks::generators::java::java_blocks_generator::JavaBlocksGenerator;

    #[test]
    fn test_comments() {
        let code = "// line\n/* block */";
        let blocks = JavaBlocksGenerator::new(code.to_owned()).generate().unwrap();
        assert_eq!(blocks[0].op_code, "line_comment");
        assert_eq!(blocks[0].parameters[0], " line");
        assert_eq!(blocks[1].op_code, "block_comment");
        assert_eq!(blocks[1].parameters[0], " block ");
    }
}
