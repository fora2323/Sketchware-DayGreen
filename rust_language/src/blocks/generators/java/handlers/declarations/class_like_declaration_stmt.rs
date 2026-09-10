use crate::blocks::block_palette::BlockBean;
use crate::blocks::generators::java::coordinator::generation_coordinator::{
    DispatchType, GenerationCoordinator, StatementHandler,
};
use tree_sitter::Node;

pub struct ClassLikeDeclarationStatement;

struct TypeDeclSpec {
    can_have_type_params: bool,
    can_have_extends: bool,
    can_have_implements: bool,
    body_field: &'static str,
    extends_is_interface: bool,
}

impl TypeDeclSpec {
    fn new(kind: &str) -> Self {
        match kind {
            "class_declaration" => Self {
                can_have_type_params: true,
                can_have_extends: true,
                can_have_implements: true,
                body_field: "class_body",
                extends_is_interface: false,
            },
            "interface_declaration" => Self {
                can_have_type_params: true,
                can_have_extends: true,
                can_have_implements: false,
                body_field: "interface_body",
                extends_is_interface: true,
            },
            "enum_declaration" => Self {
                can_have_type_params: false,
                can_have_extends: false,
                can_have_implements: true,
                body_field: "enum_body",
                extends_is_interface: false,
            },
            "record_declaration" => Self {
                can_have_type_params: true,
                can_have_extends: false,
                can_have_implements: true,
                body_field: "class_body",
                extends_is_interface: false,
            },
            "annotation_type_declaration" => Self {
                can_have_type_params: false,
                can_have_extends: false,
                can_have_implements: false,
                body_field: "annotation_type_body",
                extends_is_interface: false,
            },
            _ => Self {
                can_have_type_params: false,
                can_have_extends: false,
                can_have_implements: false,
                body_field: "class_body",
                extends_is_interface: false,
            },
        }
    }
}

impl StatementHandler for ClassLikeDeclarationStatement {
    fn get_block(kind: &str) -> BlockBean {
        BlockBean::new()
            .set_color(Some(0xFFE1A92A))
            .set_op_code(kind)
            .set_type("c")
            .set_code("")
            .set_spec(match kind {
                "class_declaration" => {
                    "%m.modifiers class %m.name %m.typeParameters %m.extends %m.implements"
                }
                "interface_declaration" => {
                    "%m.modifiers interface %m.interfaceName %m.typeParameters %m.extends"
                }
                "enum_declaration" => "%m.modifiers enum %m.enumName %m.implements",
                "record_declaration" => {
                    "%m.modifiers record %m.recordName %m.typeParameters %m.implements"
                }
                "annotation_type_declaration" => "%m.modifiers @interface %m.annotationName",
                _ => "%m.modifiers class %m.name %m.typeParameters %m.extends %m.implements",
            })
    }

    fn handle(
        node: &Node,
        generator_coordinator: &mut GenerationCoordinator,
        dispatch_type: DispatchType,
    ) {
        let op_code = node.kind();
        let spec = TypeDeclSpec::new(op_code);

        let modifiers = extract_modifiers(node, generator_coordinator);
        let name = extract_identifier(node, generator_coordinator);
        let type_params = extract_type_params(node, generator_coordinator, &spec);
        let extends = extract_extends(node, generator_coordinator, &spec);
        let implements = extract_implements(node, generator_coordinator, &spec);

        let mut bean = Self::get_block(op_code);

        let next_id = generator_coordinator.get_next_id();
        bean.sub_stack1 = next_id;

        bean.parameters.push(modifiers);
        bean.parameters.push(name);

        match op_code {
            "class_declaration" => {
                bean.parameters.push(type_params);
                bean.parameters.push(extends);
                bean.parameters.push(implements);
            }
            "interface_declaration" => {
                bean.parameters.push(type_params);
                bean.parameters.push(extends);
            }
            "enum_declaration" => {
                bean.parameters.push(implements);
            }
            "record_declaration" => {
                bean.parameters.push(type_params);
                bean.parameters.push(implements);
            }
            _ => {}
        }

        generator_coordinator.push(bean, dispatch_type);
        body_process(node, generator_coordinator, &spec);
    }
}

fn body_process(node: &Node, ctx: &mut GenerationCoordinator, spec: &TypeDeclSpec) {
    let mut cursor = node.walk();
    for child in node.children(&mut cursor) {
        if child.kind() == spec.body_field {
            let total = child.child_count();

            if total > 2 {
                let end = total - 1;
                let processed = end - 1;

                for (pos, i) in (1..end).enumerate() {
                    if let Some(grandchild) = child.child(i) {
                        ctx.dispatch(&grandchild, pos, processed);
                    }
                }
            }
        }
    }
}

fn extract_modifiers(node: &Node, ctx: &mut GenerationCoordinator) -> String {
    let mut cursor = node.walk();
    for child in node.children(&mut cursor) {
        if child.kind() == "modifiers" {
            let mut out = String::new();
            for i in 0..child.child_count() {
                if let Some(c) = child.child(i) {
                    match c.kind() {
                        "marker_annotation" | "annotation" => {
                            ctx.dispatch_with_type(&c, DispatchType::NextBlock);
                        }
                        _ => {
                            if !out.is_empty() {
                                out.push(' ');
                            }
                            out.push_str(ctx.get_code(&c));
                        }
                    }
                }
            }
            return out;
        }
    }
    String::new()
}

fn extract_identifier(node: &Node, ctx: &GenerationCoordinator) -> String {
    let mut cursor = node.walk();
    for child in node.children(&mut cursor) {
        if child.kind() == "identifier" {
            return ctx.get_code(&child).to_string();
        }
    }
    String::new()
}

fn extract_type_params(node: &Node, ctx: &GenerationCoordinator, spec: &TypeDeclSpec) -> String {
    if !spec.can_have_type_params {
        return String::new();
    }

    let mut cursor = node.walk();
    for child in node.children(&mut cursor) {
        if child.kind() == "type_parameters" {
            let code = ctx.get_code_with_range(&child, 1..);
            return if !code.is_empty() {
                code[..code.len() - 1].to_string()
            } else {
                code.to_string()
            };
        }
    }
    String::new()
}

fn extract_extends(node: &Node, ctx: &GenerationCoordinator, spec: &TypeDeclSpec) -> String {
    if !spec.can_have_extends {
        return String::new();
    }

    let target = if spec.extends_is_interface {
        "extends_interfaces"
    } else {
        "superclass"
    };

    let mut cursor = node.walk();
    for child in node.children(&mut cursor) {
        if child.kind() == target {
            return ctx.get_code_with_range(&child, 8..).to_string();
        }
    }
    String::new()
}

fn extract_implements(node: &Node, ctx: &GenerationCoordinator, spec: &TypeDeclSpec) -> String {
    if !spec.can_have_implements {
        return String::new();
    }

    let mut cursor = node.walk();
    for child in node.children(&mut cursor) {
        if child.kind() == "super_interfaces" {
            let mut out = String::new();
            collect_identifiers(&child, ctx, &mut out);
            return out;
        }
    }
    String::new()
}

fn collect_identifiers(node: &Node, ctx: &GenerationCoordinator, out: &mut String) {
    if node.kind() == "type_identifier" {
        if !out.is_empty() {
            out.push_str(", ");
        }
        out.push_str(ctx.get_code(node));
    }
    for i in 0..node.child_count() {
        if let Some(c) = node.child(i) {
            collect_identifiers(&c, ctx, out);
        }
    }
}

#[cfg(test)]
mod tests {
    use crate::blocks::generators::java::java_blocks_generator::JavaBlocksGenerator;

    #[test]
    fn test_class_declaration() {
        let code = "public class MyClass extends Base implements I1, I2 { // comment \n }";
        let blocks = JavaBlocksGenerator::new(code.to_owned())
            .generate()
            .unwrap();
        assert_eq!(blocks[0].op_code, "class_declaration");
        assert_eq!(blocks[0].parameters[0], "public");
        assert_eq!(blocks[0].parameters[1], "MyClass");
        assert_eq!(blocks[0].parameters[3], "Base");
        assert_eq!(blocks[0].parameters[4], "I1, I2");
        assert_eq!(blocks[1].op_code, "line_comment");
    }
}
