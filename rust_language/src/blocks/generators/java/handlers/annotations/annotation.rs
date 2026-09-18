use crate::blocks::block_palette::BlockBean;
use crate::blocks::generators::java::coordinator::generation_coordinator::{
    DispatchType, GenerationCoordinator, StatementHandler,
};
use tree_sitter::Node;

pub struct AnnotationSubStmt;

impl StatementHandler for AnnotationSubStmt {
    fn get_block(kind: &str) -> BlockBean {
        BlockBean::new()
            .set_color(Some(0xFFE1A92A))
            .set_op_code(kind)
            .set_spec(&(kind.to_owned() + " %asd"))
            .set_code(&(kind.to_owned() + " %s"))
            .set_type(" ")
    }

    fn handle(
        node: &Node,
        generator_coordinator: &mut GenerationCoordinator,
        dispatch_type: DispatchType,
    ) {
        let full = generator_coordinator.get_code(node);

        let mut name = full;
        let mut fields_text = String::new();

        if let Some(start) = full.find('(') {
            name = &full[..start];
            if let Some(end) = full.rfind(')') {
                fields_text = full[start + 1..end].trim().to_string();
            }
        }

        let mut bean = Self::get_block(name);

        if !fields_text.is_empty() {
            bean.parameters.push(fields_text);
        }

        generator_coordinator.push(bean, dispatch_type);
    }
}

#[cfg(test)]
mod tests {
    use crate::blocks::generators::java::java_blocks_generator::JavaBlocksGenerator;

    #[test]
    fn test_annotation() {
        let code = "@MyAnno(a=1, b=2) public static void hi() {}";
        let blocks = JavaBlocksGenerator::new(code.to_owned()).generate().unwrap();
        assert_eq!(blocks[0].op_code, "@MyAnno");
        assert_eq!(blocks[0].parameters[0], "a=1, b=2");
    }
}
