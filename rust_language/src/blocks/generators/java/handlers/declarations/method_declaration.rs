use crate::blocks::block_palette::BlockBean;
use crate::blocks::generators::java::coordinator::generation_coordinator::{
    DispatchType, GenerationCoordinator, StatementHandler,
};
use tree_sitter::Node;

pub struct MethodDeclarationStatement;

impl StatementHandler for MethodDeclarationStatement {
    fn get_block(kind: &str) -> BlockBean {
        BlockBean::new()
            .set_op_code(kind)
            .set_type("c")
            .set_color(Some(0xFFE1A92A))
    }

    fn handle(
        node: &Node,
        generator_coordinator: &mut GenerationCoordinator,
        dispatch_type: DispatchType,
    ) {
        let header = MethodHeader::from(node, generator_coordinator);
        let mut spec = String::new();
        let mut params: Vec<String> = Vec::new();

        if !header.modifiers.is_empty() {
            spec.push_str("%m.modifiers ");
            params.push(header.modifiers.clone());
        }

        if let Some(ref tp) = header.type_parameters {
            spec.push_str("%m.typeParameters ");
            params.push(generator_coordinator.get_code(tp).to_string());
        }

        spec.push_str("%m.returnType ");
        params.push(generator_coordinator.get_code(&header.return_type).to_string());

        spec.push_str("%m.name ");
        params.push(generator_coordinator.get_code(&header.name).to_string());

        spec.push_str(&build_params_spec(&header.parameters, generator_coordinator));

        if let Some(ref dims) = header.dimensions {
            spec.push(' ');
            spec.push_str("%m.dimensions");
            params.push(dims.clone());
        }

        if let Some(ref throws) = header.throws {
            spec.push_str(" throws %m.exception");
            params.push(
                generator_coordinator
                    .get_code_with_range(throws, 7..)
                    .trim()
                    .to_string(),
            );
        }

        let spec_str = spec.trim().to_string();

        let mut bean = Self::get_block(node.kind());
        bean.spec = spec_str;
        bean.parameters = params;

        let bean_id = generator_coordinator.push(bean, dispatch_type);

        if let Some(body) = header.body {
            let total = body.child_count();
            if total > 2 {
                let next_id = generator_coordinator.get_next_id();
                generator_coordinator.borrow_mut_block(bean_id, |bean| {
                    bean.sub_stack1 = next_id;
                });

                let end = total - 1;
                let processed = end - 1;
                for (pos, i) in (1..end).enumerate() {
                    if let Some(child) = body.child(i) {
                        generator_coordinator.dispatch(&child, pos, processed);
                    }
                }
            }
        }

        let next_id = generator_coordinator.get_next_id();
        generator_coordinator.borrow_mut_block(bean_id, |bean| {
            if bean.next_block != -1 {
                bean.next_block = next_id;
            }
        });
    }
}

fn build_params_spec(params_node: &Node, ctx: &GenerationCoordinator) -> String {
    let mut parts = Vec::new();
    let mut cursor = params_node.walk();

    for child in params_node.named_children(&mut cursor) {
        match child.kind() {
            "formal_parameter" => build_formal_param(&child, ctx, &mut parts),
            "spread_parameter" => build_spread_param(&child, ctx, &mut parts),
            "receiver_parameter" => build_receiver_param(&child, ctx, &mut parts),
            _ => {
                parts.push(format!("%m.{}", ctx.get_code(&child)));
            }
        }
    }

    if parts.is_empty() {
        return "()".to_string();
    }

    format!("( {} )", parts.join(" , "))
}

fn build_formal_param(node: &Node, ctx: &GenerationCoordinator, parts: &mut Vec<String>) {
    let type_node = node.child_by_field_name("type").unwrap();
    let name_node = node.child_by_field_name("name").unwrap();
    let type_text = ctx.get_code(&type_node);
    let name_text = ctx.get_code(&name_node);

    let dims = node
        .child_by_field_name("dimensions")
        .map(|d| ctx.get_code(&d).to_string())
        .unwrap_or_default();
    let full_type = format!("{}{}", type_text, dims);

    parts.push(format_param_entry(&full_type, name_text, false));
}

fn build_spread_param(node: &Node, ctx: &GenerationCoordinator, parts: &mut Vec<String>) {
    let mut type_text = String::new();
    let mut name_text = String::new();

    let mut inner_cursor = node.walk();
    for child in node.named_children(&mut inner_cursor) {
        match child.kind() {
            "variable_declarator" => {
                if let Some(name_node) = child.child_by_field_name("name") {
                    name_text = ctx.get_code(&name_node).to_string();
                }
            }
            "modifiers" => {}
            _ => {
                type_text = ctx.get_code(&child).to_string();
            }
        }
    }

    parts.push(format_param_entry(&type_text, &name_text, true));
}

fn build_receiver_param(node: &Node, ctx: &GenerationCoordinator, parts: &mut Vec<String>) {
    parts.push(format!("%m.{}", ctx.get_code(node)));
}

fn format_param_entry(type_text: &str, name_text: &str, is_spread: bool) -> String {
    let entry = match type_text {
        "String" => format!("%s.{}", name_text),
        "int" | "long" | "short" | "byte" | "float" | "double" => format!("%d.{}", name_text),
        "boolean" => format!("%b.{}", name_text),
        other => format!("%m.{}.{}", other, name_text),
    };
    if is_spread {
        format!("{}...", entry)
    } else {
        entry
    }
}

pub struct MethodHeader<'a> {
    pub modifiers: String,
    pub type_parameters: Option<Node<'a>>,
    pub return_type: Node<'a>,
    pub name: Node<'a>,
    pub parameters: Node<'a>,
    pub dimensions: Option<String>,
    pub throws: Option<Node<'a>>,
    pub body: Option<Node<'a>>,
}

impl<'a> MethodHeader<'a> {
    pub fn from(node: &Node<'a>, generator_coordinator: &mut GenerationCoordinator) -> Self {
        let name = node.child_by_field_name("name").unwrap();
        let parameters = node.child_by_field_name("parameters").unwrap();
        let return_type = node.child_by_field_name("type").unwrap();

        let body = node.child_by_field_name("body");
        let type_parameters = node.child_by_field_name("type_parameters");
        let dimensions = node
            .child_by_field_name("dimensions")
            .map(|dimensions| generator_coordinator.get_code(&dimensions).to_string());

        let mut modifiers = Vec::new();
        let mut throws_node = None;

        let mut cursor = node.walk();
        for child in node.named_children(&mut cursor) {
            match child.kind() {
                "modifiers" => {
                    for grandchild in child.children(&mut child.walk()) {
                        match grandchild.kind() {
                            "marker_annotation" | "annotation" => {
                                generator_coordinator
                                    .dispatch_with_type(&grandchild, DispatchType::NextBlock);
                            }
                            _ => {
                                modifiers.push(generator_coordinator.get_code(&grandchild).to_string())
                            }
                        }
                    }
                }
                "annotation" | "marker_annotation" => {
                    generator_coordinator.dispatch_with_type(&child, DispatchType::NextBlock)
                }
                "throws" => throws_node = Some(child),
                _ => {}
            }
        }

        MethodHeader {
            modifiers: modifiers.join(" "),
            type_parameters,
            return_type,
            name,
            parameters,
            dimensions,
            throws: throws_node,
            body,
        }
    }
}

#[cfg(test)]
mod tests {
    use crate::blocks::generators::java::java_blocks_generator::JavaBlocksGenerator;

    #[test]
    fn test_crazy_method() {
        let code = r#"
        @Override
        public static <T> String[][] crazyMethod(int a, T... items)[][]
            throws IOException, IllegalStateException {

            return new String[0][0];
        }
        "#
            .to_owned();
        let blocks = JavaBlocksGenerator::new(code).generate().unwrap();

        assert_eq!(blocks[0].op_code, "@Override");
        assert_eq!(blocks[1].op_code, "method_declaration");
        assert_eq!(blocks[1].parameters[0], "public static");
        assert_eq!(blocks[1].parameters[1], "<T>");
        assert_eq!(blocks[1].parameters[2], "String[][]");
        assert_eq!(blocks[1].parameters[3], "crazyMethod");
        assert!(blocks[1].spec.contains("( %d.a , %m.T.items... )"));
        assert!(blocks[1].spec.contains("throws %m.exception"));

        dbg!(blocks);
    }

    #[test]
    fn test_simple_method() {
        let code = "void hello() { // hi }";
        let blocks = JavaBlocksGenerator::new(code.to_owned()).generate().unwrap();
        assert_eq!(blocks[0].op_code, "method_declaration");
        assert!(blocks[0].spec.contains("%m.returnType"));
        assert!(blocks[0].spec.contains("()"));
        assert!(!blocks[0].spec.contains("throws"));
    }

    #[test]
    fn test_method_with_params() {
        let code = "String greet(String name, int age) { return true; }";
        let blocks = JavaBlocksGenerator::new(code.to_owned()).generate().unwrap();
        assert_eq!(blocks[0].op_code, "method_declaration");
        assert!(blocks[0].spec.contains("( %s.name , %d.age )"));
    }

    #[test]
    fn test_method_with_throws_only() {
        let code = "void risky() throws Exception { // hi }";
        let blocks = JavaBlocksGenerator::new(code.to_owned()).generate().unwrap();
        assert_eq!(blocks[0].op_code, "method_declaration");
        assert!(blocks[0].spec.contains("throws %m.exception"));
        assert!(blocks[0].parameters.iter().any(|p| p == "Exception"));
        assert_eq!(blocks[1].op_code, "line_comment");
    }
}
