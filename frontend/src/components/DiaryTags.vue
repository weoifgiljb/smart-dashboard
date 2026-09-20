<script lang="ts">
/** 禁止把 h(ElTag) 插进模板 {{ }}，会触发 circular JSON 把日记列表打白。 */
import { defineComponent, h, type PropType } from 'vue'
import { ElTag } from 'element-plus'

export default defineComponent({
  name: 'DiaryTags',
  props: {
    tags: {
      type: Array as PropType<string[]>,
      default: () => [],
    },
  },
  setup(props) {
    return () =>
      h(
        'div',
        { class: 'diary-tags' },
        props.tags.map((tag) =>
          h(ElTag, { key: tag, size: 'small', class: 'tag-item' }, () => `# ${tag}`),
        ),
      )
  },
})
</script>

<style scoped lang="less">
.diary-tags {
  margin-top: 12px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tag-item {
  border-radius: 12px;
}
</style>
