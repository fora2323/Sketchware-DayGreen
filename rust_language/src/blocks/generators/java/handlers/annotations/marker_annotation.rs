use crate::blocks::block_palette::BlockBean;
use crate::blocks::generators::java::coordinator::generation_coordinator::{
    DispatchType, GenerationCoordinator, StatementHandler,
};
use tree_sitter::Node;

pub struct MarkerAnnotationSubStmt;

impl StatementHandler for MarkerAnnotationSubStmt {
    fn get_block(kind: &str) -> BlockBean {
        BlockBean::new()
            .set_color(Some(0xFFE1A92A))
            .set_op_code(kind)
            .set_spec(kind)
            .set_code(kind)
            .set_type(" ")
    }

    fn handle(
        node: &Node,
        generator_coordinator: &mut GenerationCoordinator,
        dispatch_type: DispatchType,
    ) {
        let annotation = generator_coordinator.get_code(node);
        let bean = Self::get_block(annotation);

        generator_coordinator.push(bean, dispatch_type);
    }
}

#[cfg(test)]
mod tests {
    use crate::blocks::generators::java::java_blocks_generator::JavaBlocksGenerator;

    #[test]
    fn test_marker_annotation() {
        let code = "@Deprecated public static void hi() {}";
        let blocks = JavaBlocksGenerator::new(code.to_owned()).generate().unwrap();
        assert_eq!(blocks[0].op_code, "@Deprecated");
    }
}
