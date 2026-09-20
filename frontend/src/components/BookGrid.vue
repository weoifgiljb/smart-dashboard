<script lang="ts">
import { defineComponent, h, type PropType } from 'vue'
import BookCard from '@/components/BookCard.vue'
import type { BookItem } from '@/utils/bookDisplay'

export default defineComponent({
  name: 'BookGrid',
  props: {
    books: {
      type: Array as PropType<BookItem[]>,
      required: true,
    },
    favoriteIds: {
      type: Object as PropType<Set<string>>,
      required: true,
    },
    generatingId: {
      type: String as PropType<string | null>,
      default: null,
    },
  },
  emits: {
    open: (_book: BookItem) => true,
    toggleFavorite: (_book: BookItem) => true,
    generate: (_book: BookItem) => true,
  },
  setup(props, { emit }) {
    return () =>
      h(
        'div',
        { class: 'masonry-grid' },
        props.books.map((book) =>
          h(BookCard, {
            key: book.id,
            book,
            favorited: props.favoriteIds.has(book.id),
            generating: props.generatingId === book.id,
            onOpen: (item: BookItem) => emit('open', item),
            onToggleFavorite: (item: BookItem) => emit('toggleFavorite', item),
            onGenerate: (item: BookItem) => emit('generate', item),
          }),
        ),
      )
  },
})
</script>

<style scoped lang="less">
.masonry-grid {
  column-count: 4;
  column-gap: 24px;
}

@media (max-width: 992px) {
  .masonry-grid {
    column-count: 3;
  }
}
@media (max-width: 768px) {
  .masonry-grid {
    column-count: 2;
  }
}
@media (max-width: 480px) {
  .masonry-grid {
    column-count: 1;
  }
}
</style>
