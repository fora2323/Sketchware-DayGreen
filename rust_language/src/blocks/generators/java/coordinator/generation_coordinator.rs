use crate::blocks::block_palette::BlockBean;
use tree_sitter::Node;

#[derive(PartialEq, Eq, Clone, Copy)]
pub enum DispatchType<'a> {
    NextBlock,
    BlockParam(&'a str),
    LastChild,
}

pub struct GenerationCoordinator {
    pub code: String,
    id_counter: i32,
    generated_blocks: Vec<BlockBean>,
}

pub trait StatementHandler {
    fn get_block(kind: &str) -> BlockBean;

    fn handle(
        node: &Node,
        generator_coordinator: &mut GenerationCoordinator,
        dispatch_type: DispatchType,
    );
}

impl GenerationCoordinator {
    pub fn new(code: String) -> Self {
        Self {
            code,
            id_counter: 10,
            generated_blocks: Vec::new(),
        }
    }

    pub fn push(&mut self, mut bean: BlockBean, dispatch_type: DispatchType) -> usize {
        let id = self.get_and_increase_id();
        let next_block = if dispatch_type == DispatchType::NextBlock {
            self.get_next_id()
        } else {
            -1
        };

        bean.id = id;
        bean.next_block = next_block;
        self.generated_blocks.push(bean);

        self.generated_blocks.len() - 1
    }

    pub fn get_next_id(&self) -> i32 {
        self.id_counter
    }

    pub fn get_and_increase_id(&mut self) -> i32 {
        let id = self.id_counter;
        self.id_counter += 1;
        id
    }

    pub fn borrow_mut_block<F>(&mut self, index: usize, editor: F)
    where
        F: FnOnce(&mut BlockBean),
    {
        editor(&mut self.generated_blocks[index]);
    }

    pub fn build(mut self) -> Vec<BlockBean> {
        let existing_ids: std::collections::HashSet<i32> = self.generated_blocks.iter().map(|b| b.id).collect();
        for block in &mut self.generated_blocks {
            if block.next_block >= 0 && !existing_ids.contains(&block.next_block) {
                block.next_block = -1;
            }
            if block.sub_stack1 >= 0 && !existing_ids.contains(&block.sub_stack1) {
                block.sub_stack1 = -1;
            }
            if block.sub_stack2 >= 0 && !existing_ids.contains(&block.sub_stack2) {
                block.sub_stack2 = -1;
            }
        }
        self.generated_blocks
    }
}
