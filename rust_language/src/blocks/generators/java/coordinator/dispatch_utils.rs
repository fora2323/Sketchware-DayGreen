use super::generation_coordinator::{DispatchType, GenerationCoordinator, StatementHandler};
use crate::blocks::block_palette::BlockBean;
use tree_sitter::Node;

use crate::blocks::generators::java::handlers::{
    annotations::{annotation::AnnotationSubStmt, marker_annotation::MarkerAnnotationSubStmt},
    comments::comments_stmt::CommentStatement,
    declarations::{
        class_like_declaration_stmt::ClassLikeDeclarationStatement,
        import_declaration_stmt::ImportDeclarationStatement,
        local_variable_declaration::LocalVariableDeclarationStatement,
        method_declaration::MethodDeclarationStatement,
        package_declaration::PackageDeclarationStatement,
    },
    expressions::{
        assignment_expression::AssignmentExpressionHandler,
        binary_expression::BinaryExpressionHandler, cast_expression::CastStatementHandler,
        expression_statement::ExpressionStatementHandler,
        instanceof_expression::InstanceOfExpressionHandler,
        method_invocation::MethodInvocationHandler,
        parenthesized_expression::ParenthesizedExpression,
        ternary_expression::TernaryExpressionHandler, unary_expression::UnaryExpressionHandler,
        update_expression::UpdateExpressionHandler,
    },
    fallback_stmt::FallbackStatement,
    literal::{
        literal_with_wrap_param::LiteralWithWrapParamStatement,
        literal_without_params::LiteralWithoutParamsStatement,
    },
    statements::{
        break_continue_stmt::BreakContinueStatement, for_statement::ForStatement,
        if_else_stmt::IfElseStatement, return_statement::ReturnStatement,
        while_statement::WhileStatement,
    },
};

use phf::phf_map;

type HandlerFn = fn(&Node, &mut GenerationCoordinator, DispatchType);

static HANDLERS: phf::Map<&'static str, HandlerFn> = phf_map! {
    // Annotations
    "marker_annotation" => MarkerAnnotationSubStmt::handle,
    "annotation" => AnnotationSubStmt::handle,

    // Comments
    "line_comment" => CommentStatement::handle,
    "block_comment" => CommentStatement::handle,

    // Declarations
    "import_declaration" => ImportDeclarationStatement::handle,
    "package_declaration" => PackageDeclarationStatement::handle,
    "class_declaration" => ClassLikeDeclarationStatement::handle,
    "interface_declaration" => ClassLikeDeclarationStatement::handle,
    "enum_declaration" => ClassLikeDeclarationStatement::handle,
    "record_declaration" => ClassLikeDeclarationStatement::handle,
    "annotation_type_declaration" => ClassLikeDeclarationStatement::handle,
    "method_declaration" => MethodDeclarationStatement::handle,
    "local_variable_declaration" => LocalVariableDeclarationStatement::handle,

    // Statements
    "if_statement" => IfElseStatement::handle,
    "while_statement" => WhileStatement::handle,
    "for_statement" => ForStatement::handle,
    "return_statement" => ReturnStatement::handle,
    "break_statement" => BreakContinueStatement::handle,
    "continue_statement" => BreakContinueStatement::handle,

    // Expressions
    "assignment_expression" => AssignmentExpressionHandler::handle,
    "method_invocation" => MethodInvocationHandler::handle,
    "cast_expression" => CastStatementHandler::handle,
    "binary_expression" => BinaryExpressionHandler::handle,
    "unary_expression" => UnaryExpressionHandler::handle,
    "ternary_expression" => TernaryExpressionHandler::handle,
    "update_expression" => UpdateExpressionHandler::handle,
    "instanceof_expression" => InstanceOfExpressionHandler::handle,
    "expression_statement" => ExpressionStatementHandler::handle,
    "parenthesized_expression" => ParenthesizedExpression::handle,

    // Literals
    "true" => LiteralWithoutParamsStatement::handle,
    "false" => LiteralWithoutParamsStatement::handle,
    "null_literal" => LiteralWithoutParamsStatement::handle,
    "hex_integer_literal" => LiteralWithWrapParamStatement::handle,
    "hex_floating_point_literal" => LiteralWithWrapParamStatement::handle,
    "binary_integer_literal" => LiteralWithWrapParamStatement::handle,
};

impl GenerationCoordinator {
    pub fn dispatch_as_block_param(
        &mut self,
        node: Node,
        bean_id: usize,
        dispatch_type: DispatchType,
    ) {
        let (param, can_dispatch) = if matches!(
            node.kind(),
            "character_literal"
                | "decimal_floating_point_literal"
                | "decimal_integer_literal"
                | "octal_integer_literal"
                | "string_literal"
        ) {
            let mut code = self.get_code(&node).to_string();
            if node.kind() == "string_literal" {
                if code.starts_with('"') && code.ends_with('"') && code.len() >= 2 {
                    code = code[1..code.len() - 1].to_string();
                }
            }
            (code, false)
        } else if node.kind() == "identifier" {
            let ident_name = self.get_code(&node).trim().to_string();
            let is_menu_slot = match dispatch_type {
                DispatchType::BlockParam(t) => t.starts_with("m.") || t == "var" || t == "view" || t == "intent" || t == "list",
                _ => false,
            };

            if is_menu_slot {
                (ident_name, false)
            } else {
                let next_id = self.get_next_id();
                let expected_t = match dispatch_type {
                    DispatchType::BlockParam("b") => "b",
                    DispatchType::BlockParam("s") => "s",
                    _ => "d",
                };
                let get_var_bean = BlockBean::new()
                    .set_op_code("getVar")
                    .set_type(expected_t)
                    .set_spec(&ident_name)
                    .set_color(Some(0xFFEE7D16));
                self.push(get_var_bean, DispatchType::LastChild);
                (format!("@{}", next_id), false)
            }
        } else if node.kind() == "field_access" {
            let code = self.get_code(&node).trim().to_string();
            if code == "Math.PI" {
                let next_id = self.get_next_id();
                let bean = BlockBean::new()
                    .set_op_code("mathPi")
                    .set_type("d")
                    .set_spec("PI")
                    .set_color(Some(0xFF23B9A9));
                self.push(bean, DispatchType::LastChild);
                (format!("@{}", next_id), false)
            } else if code == "Math.E" {
                let next_id = self.get_next_id();
                let bean = BlockBean::new()
                    .set_op_code("mathE")
                    .set_type("d")
                    .set_spec("E")
                    .set_color(Some(0xFF23B9A9));
                self.push(bean, DispatchType::LastChild);
                (format!("@{}", next_id), false)
            } else if code.contains("VISIBLE") {
                ("VISIBLE".to_string(), false)
            } else if code.contains("INVISIBLE") {
                ("INVISIBLE".to_string(), false)
            } else if code.contains("GONE") {
                ("GONE".to_string(), false)
            } else {
                let param_entry_annotation = '@';
                let next_id = self.get_next_id();
                (format!("{}{}", param_entry_annotation, next_id), true)
            }
        } else {
            let param_entry_annotation = '@';
            let next_id = self.get_next_id();
            (format!("{}{}", param_entry_annotation, next_id), true)
        };
        self.borrow_mut_block(bean_id, |bean| {
            bean.parameters.push(param);
        });
        if can_dispatch {
            self.dispatch_with_type(&node, dispatch_type);
        }
    }

    pub fn dispatch(&mut self, node: &Node, index: usize, child_count: usize) {
        let dispatch_type = if index == (child_count - 1) {
            DispatchType::LastChild
        } else {
            DispatchType::NextBlock
        };
        self.dispatch_with_type(node, dispatch_type);
    }

    pub fn dispatch_with_type(&mut self, node: &Node, dispatch_type: DispatchType) {
        if let Some(handler) = HANDLERS.get(node.kind()) {
            handler(node, self, dispatch_type);
        } else {
            FallbackStatement::handle(node, self, dispatch_type);
        }
    }
}
