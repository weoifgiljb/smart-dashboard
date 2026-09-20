<template>
  <div class="year-heatmap">
    <div class="heatmap-toolbar">
      <div class="heatmap-heading">
        <div class="heatmap-title">
          <slot name="title">年度活动热力</slot>
        </div>
        <p class="heatmap-meta">{{ year }} 年 · {{ activeDays }} 天有活动</p>
      </div>
      <div class="heatmap-legend" aria-hidden="true">
        <span>少</span>
        <i class="swatch l0" />
        <i class="swatch l1" />
        <i class="swatch l2" />
        <i class="swatch l3" />
        <i class="swatch l4" />
        <span>多</span>
      </div>
    </div>

    <div class="heatmap-scroll">
      <div class="heatmap-board">
        <div class="weekday-col" aria-hidden="true">
          <span class="weekday spacer"></span>
          <span v-for="(label, idx) in weekdayLabels" :key="idx" class="weekday">{{ label }}</span>
        </div>
        <div class="heatmap-grid">
          <div class="month-row">
            <span
              v-for="(label, idx) in monthLabels"
              :key="`m-${idx}`"
              class="month-label"
              :class="{ empty: !label }"
              >{{ label }}</span
            >
          </div>
          <div class="weeks">
            <div v-for="(week, wi) in weeks" :key="`w-${wi}`" class="week">
              <button
                v-for="cell in week"
                :key="cell.key"
                type="button"
                class="heat-cell"
                :class="[`l${cell.level}`, { pad: !cell.date, today: cell.date === todayKey }]"
                :disabled="!cell.date"
                :aria-label="cellLabel(cell)"
                :title="cellLabel(cell)"
                @click="cell.date && emit('select', cell.date)"
              />
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { buildYearHeatmap, type DayActivity, type HeatCell } from '@/utils/yearHeatmap'

const props = withDefaults(
  defineProps<{
    year: number
    activity: Record<string, DayActivity>
    weekStartsOn?: number
    todayKey?: string
  }>(),
  {
    weekStartsOn: 1,
    todayKey: '',
  },
)

const emit = defineEmits<{
  select: [date: string]
}>()

const built = computed(() => buildYearHeatmap(props.year, props.activity, props.weekStartsOn))
const weeks = computed(() => built.value.weeks)
const monthLabels = computed(() => built.value.monthLabels)
const activeDays = computed(() => built.value.activeDays)

const weekdayLabels = computed(() => {
  const all =
    props.weekStartsOn === 1
      ? ['一', '二', '三', '四', '五', '六', '日']
      : ['日', '一', '二', '三', '四', '五', '六']
  return all.map((label, idx) => (idx % 2 === 0 ? label : ''))
})

function cellLabel(cell: HeatCell) {
  if (!cell.date) return ''
  return cell.heat > 0 ? `${cell.date}：热力 ${cell.heat}` : `${cell.date}：无记录`
}
</script>

<style scoped lang="less">
.year-heatmap {
  display: flex;
  flex-direction: column;
  gap: 14px;
  min-width: 0;
}

.heatmap-toolbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.heatmap-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 16px;
  font-weight: 600;
  color: var(--app-text);
}

.heatmap-meta {
  margin: 4px 0 0;
  font-size: 12px;
  color: var(--text-secondary);
}

.heatmap-legend {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  color: var(--text-light);
  flex-shrink: 0;
  padding-top: 4px;
}

.swatch {
  width: 12px;
  height: 12px;
  border-radius: 3px;
  background: var(--color-bg-muted);
}

.heat-cell {
  width: var(--heat-cell, 13px);
  height: var(--heat-cell, 13px);
  border: none;
  padding: 0;
  display: block;
  border-radius: 3px;
  background: var(--color-bg-muted);
  cursor: pointer;
  transition:
    transform 0.12s ease,
    box-shadow 0.12s ease;
}

.swatch.l1,
.heat-cell.l1 {
  background: color-mix(in srgb, var(--color-primary) 28%, var(--color-bg-muted));
}
.swatch.l2,
.heat-cell.l2 {
  background: color-mix(in srgb, var(--color-primary) 48%, var(--color-bg-muted));
}
.swatch.l3,
.heat-cell.l3 {
  background: color-mix(in srgb, var(--color-primary) 72%, var(--color-bg-muted));
}
.swatch.l4,
.heat-cell.l4 {
  background: var(--color-primary);
}

.heatmap-scroll {
  overflow-x: auto;
  padding-bottom: 2px;
}

.heatmap-board {
  --heat-cell: 13px;
  --heat-gap: 4px;
  display: flex;
  gap: 10px;
  width: max-content;
  min-width: 100%;
}

.weekday-col {
  display: grid;
  grid-template-rows: 18px repeat(7, var(--heat-cell));
  gap: var(--heat-gap);
  font-size: 10px;
  line-height: var(--heat-cell);
  color: var(--text-light);
  text-align: right;
  flex-shrink: 0;
}

.weekday.spacer {
  height: 18px;
}

.heatmap-grid {
  display: flex;
  flex-direction: column;
  gap: var(--heat-gap);
}

.month-row {
  display: grid;
  grid-auto-flow: column;
  grid-auto-columns: var(--heat-cell);
  gap: var(--heat-gap);
  height: 18px;
  font-size: 11px;
  color: var(--text-secondary);
}

.month-label {
  white-space: nowrap;
}

.month-label.empty {
  visibility: hidden;
}

.weeks {
  display: grid;
  grid-auto-flow: column;
  grid-auto-columns: var(--heat-cell);
  gap: var(--heat-gap);
}

.week {
  display: grid;
  grid-template-rows: repeat(7, var(--heat-cell));
  gap: var(--heat-gap);
}

.heat-cell:hover:not(:disabled) {
  transform: scale(1.18);
  box-shadow:
    0 0 0 1px var(--color-bg-elevated),
    0 0 0 2px var(--color-primary);
  z-index: 1;
}

.heat-cell.today:not(.pad) {
  box-shadow:
    0 0 0 1px var(--color-bg-elevated),
    0 0 0 2px var(--color-secondary);
}

.heat-cell.pad,
.heat-cell:disabled {
  cursor: default;
  opacity: 0;
  pointer-events: none;
}

@media (max-width: 768px) {
  .heatmap-toolbar {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
