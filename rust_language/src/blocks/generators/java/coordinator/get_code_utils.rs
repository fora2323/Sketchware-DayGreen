use super::generation_coordinator::GenerationCoordinator;
use std::ops::RangeBounds;
use tree_sitter::Node;

impl GenerationCoordinator {
    pub fn get_code(&self, node: &Node) -> &str {
        let range = node.byte_range();
        &self.code[range]
    }

    pub fn get_code_with_range<R: RangeBounds<usize>>(&self, node: &Node, range: R) -> &str {
        let node_range = node.byte_range();

        let start = match range.start_bound() {
            std::ops::Bound::Included(&s) => node_range.start + s,
            std::ops::Bound::Excluded(&s) => node_range.start + s + 1,
            std::ops::Bound::Unbounded => node_range.start,
        };

        let end = match range.end_bound() {
            std::ops::Bound::Included(&e) => node_range.start + e + 1,
            std::ops::Bound::Excluded(&e) => node_range.start + e,
            std::ops::Bound::Unbounded => node_range.end,
        };

        &self.code[start..end]
    }
}
