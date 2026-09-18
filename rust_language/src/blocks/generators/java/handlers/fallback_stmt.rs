use crate::blocks::block_palette::BlockBean;
use crate::blocks::generators::java::coordinator::generation_coordinator::{
    DispatchType, GenerationCoordinator, StatementHandler,
};
use tree_sitter::Node;

pub struct FallbackStatement;

impl StatementHandler for FallbackStatement {
    fn get_block(kind: &str) -> BlockBean {
        let (block_type, header_text, spec) = match kind {
            "String" => ("s", "", "ASD string %s"),
            "Double" => ("d", "", "ASD number %d"),
            "Bool" => ("b", "", "ASD boolean %b"),
            "Object" => ("v.Object", "", "ASD Object %s"),
            _ => (" ", "add source directly blocks", "add source directly %s"),
        };

        BlockBean::new()
            .set_op_code(&format!("addSourceDirectly{}", kind))
            .set_type(block_type)
            .set_code("%s")
            .set_spec(spec)
            .set_header_text(header_text)
            .set_color(Some(0xFF5CB722))
    }

    fn handle(
        node: &Node,
        generator_coordinator: &mut GenerationCoordinator,
        dispatch_type: DispatchType,
    ) {
        let code = generator_coordinator.get_code(node).to_string();
        let type_suffix = match dispatch_type {
            DispatchType::BlockParam(param) => match param {
                "s" => "String",
                "d" => "Double",
                "b" => "Bool",
                _ => "Object",
            },
            _ => "",
        };

        let mut bean = Self::get_block(type_suffix);
        bean.parameters.push(code);

        generator_coordinator.push(bean, dispatch_type);
    }
}
