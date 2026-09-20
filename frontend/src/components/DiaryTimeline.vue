<script lang="ts">
/** Element Plus 时间轴必须走 render 子组件：模板插值 VNode 会 JSON.stringify 循环引用，整页空白。 */
import { defineComponent, h, type PropType } from 'vue'
import { ElTimeline, ElTimelineItem } from 'element-plus'
import type { Diary } from '@/api/diary'
import DiaryEntryCard from '@/components/DiaryEntryCard.vue'
import { moodColor, parseDiaryMood } from '@/utils/diaryDisplay'

export default defineComponent({
  name: 'DiaryTimeline',
  props: {
    diaries: {
      type: Array as PropType<Diary[]>,
      required: true,
    },
  },
  emits: {
    edit: (_diary: Diary) => true,
    delete: (_diary: Diary) => true,
  },
  setup(props, { emit }) {
    return () =>
      h(ElTimeline, { class: 'diary-timeline' }, () =>
        props.diaries.map((diary) =>
          h(
            ElTimelineItem,
            {
              key: diary.id || diary.diaryDate,
              timestamp: diary.diaryDate,
              placement: 'top',
              color: moodColor(parseDiaryMood(String(diary.mood || ''))),
            },
            {
              default: () =>
                h(DiaryEntryCard, {
                  diary,
                  onEdit: (item: Diary) => emit('edit', item),
                  onDelete: (item: Diary) => emit('delete', item),
                }),
            },
          ),
        ),
      )
  },
})
</script>
