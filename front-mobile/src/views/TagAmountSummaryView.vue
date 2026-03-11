<template>
  <div>
    <!-- 기간 선택 + 태그 다중 선택 -->
    <v-card class="mb-3">
      <v-card-text class="pb-2">
        <!-- 기간 프리셋 (가로 스크롤) -->
        <div class="d-flex ga-1 mb-3" style="overflow-x: auto; white-space: nowrap;">
          <v-chip
            v-for="preset in presets"
            :key="preset.value"
            :color="selectedPreset === preset.value ? 'primary' : undefined"
            :variant="selectedPreset === preset.value ? 'flat' : 'outlined'"
            size="small"
            @click="selectedPreset = preset.value"
          >
            {{ preset.label }}
          </v-chip>
        </div>

        <!-- 시작일/종료일 -->
        <v-text-field
          v-model="startDate"
          type="date"
          label="시작일"
          density="compact"
          hide-details
          class="mb-2"
          @change="selectedPreset = 'custom'"
        />
        <v-text-field
          v-model="endDate"
          type="date"
          label="종료일"
          density="compact"
          hide-details
          class="mb-2"
          @change="selectedPreset = 'custom'"
        />

        <!-- 태그 다중 선택 -->
        <v-autocomplete
          v-model="selectedTagIds"
          :items="allTags"
          item-title="name"
          item-value="id"
          label="태그 선택 (MAIN)"
          multiple
          chips
          closable-chips
          density="compact"
          hide-details
          placeholder="태그를 선택하세요"
          class="mb-2"
        >
          <template #chip="{ props, item }">
            <v-chip v-bind="props" :color="item.raw.color" variant="flat" size="small">
              {{ item.raw.name }}
            </v-chip>
          </template>
          <template #item="{ props, item }">
            <v-list-item v-bind="props">
              <template #prepend>
                <v-icon :color="item.raw.color" size="small">mdi-circle</v-icon>
              </template>
            </v-list-item>
          </template>
        </v-autocomplete>

        <v-btn color="primary" variant="tonal" block @click="loadData" :loading="loading" :disabled="!selectedTagIds.length">
          조회
        </v-btn>
      </v-card-text>
    </v-card>

    <!-- 로딩 -->
    <div v-if="loading" class="text-center pa-8">
      <v-progress-circular indeterminate color="primary" />
    </div>

    <template v-else-if="aggregateData">
      <!-- 합산 총액 카드 -->
      <v-card class="mb-3" color="primary" variant="tonal">
        <v-card-text>
          <div class="text-overline">선택 태그 합산 총액</div>
          <div class="text-h5 font-weight-bold">{{ formatAmount(aggregateData.grandTotal) }}</div>
          <div class="text-caption">
            {{ aggregateData.startDate }} ~ {{ aggregateData.endDate }}
            · {{ aggregateData.tags.length }}개 태그 선택
          </div>
        </v-card-text>
      </v-card>

      <!-- 태그별 금액 카드 -->
      <v-card
        v-for="tag in aggregateData.tags"
        :key="tag.tagId"
        class="mb-2"
        variant="outlined"
      >
        <v-card-text class="py-2">
          <div class="d-flex align-center ga-2 mb-1">
            <v-icon :color="tag.tagColor" size="small">mdi-circle</v-icon>
            <span class="text-body-2 font-weight-medium">{{ tag.tagName }}</span>
            <v-spacer />
            <v-chip size="x-small" variant="outlined">{{ tag.percentage }}%</v-chip>
          </div>
          <div class="text-body-1 font-weight-bold">{{ formatAmount(tag.totalAmount) }}</div>
          <div class="text-caption text-grey">
            {{ tag.transactionCount }}건 · 평균 {{ formatAmount(tag.averageAmount) }}
          </div>
        </v-card-text>
      </v-card>

      <!-- 월간 추이 Line 차트 -->
      <v-card class="mt-3">
        <v-card-title class="text-body-1 font-weight-bold py-2">월간 추이</v-card-title>
        <v-card-text>
          <v-chart
            v-if="chartOption"
            :option="chartOption"
            autoresize
            style="height: 300px"
          />
          <div v-else class="text-center text-grey pa-8">월간 데이터가 없습니다</div>
        </v-card-text>
      </v-card>
    </template>

    <!-- 초기 안내 -->
    <v-card v-else-if="!loading">
      <v-card-text class="text-center text-grey pa-8">
        태그를 선택하고 조회 버튼을 눌러주세요
      </v-card-text>
    </v-card>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { fetchTags, fetchTagAggregate } from '../api'

const loading = ref(false)
const aggregateData = ref(null)
const allTags = ref([])
const selectedTagIds = ref([])

const startDate = ref('')
const endDate = ref('')
const selectedPreset = ref('thisMonth')

/** 빠른 기간 선택 프리셋 목록 */
const presets = [
  { label: '이번 달', value: 'thisMonth' },
  { label: '지난 달', value: 'lastMonth' },
  { label: '3개월', value: '3months' },
  { label: '6개월', value: '6months' },
  { label: '올해', value: 'thisYear' },
  { label: '직접 선택', value: 'custom' },
]

/** 프리셋 변경 시 시작일/종료일을 자동 설정합니다. */
watch(selectedPreset, (val) => {
  const now = new Date()
  const y = now.getFullYear()
  const m = now.getMonth()

  switch (val) {
    case 'thisMonth':
      startDate.value = formatDateStr(new Date(y, m, 1))
      endDate.value = formatDateStr(new Date(y, m + 1, 0))
      break
    case 'lastMonth':
      startDate.value = formatDateStr(new Date(y, m - 1, 1))
      endDate.value = formatDateStr(new Date(y, m, 0))
      break
    case '3months':
      startDate.value = formatDateStr(new Date(y, m - 2, 1))
      endDate.value = formatDateStr(new Date(y, m + 1, 0))
      break
    case '6months':
      startDate.value = formatDateStr(new Date(y, m - 5, 1))
      endDate.value = formatDateStr(new Date(y, m + 1, 0))
      break
    case 'thisYear':
      startDate.value = formatDateStr(new Date(y, 0, 1))
      endDate.value = formatDateStr(new Date(y, 11, 31))
      break
  }
}, { immediate: true })

const chartOption = ref(null)

/** 금액 포맷팅 (원 단위 3자리 콤마) */
function formatAmount(v) {
  return Number(v).toLocaleString('ko-KR') + '원'
}

/** Date 객체를 yyyy-MM-dd 형식 문자열로 변환합니다. */
function formatDateStr(d) {
  const yyyy = d.getFullYear()
  const mm = String(d.getMonth() + 1).padStart(2, '0')
  const dd = String(d.getDate()).padStart(2, '0')
  return `${yyyy}-${mm}-${dd}`
}

/** 태그 목록을 로드합니다. */
async function loadTags() {
  try {
    const res = await fetchTags()
    allTags.value = res.data
  } catch (e) {
    console.error('태그 목록 로딩 실패', e)
  }
}

/** 선택 태그 합산 데이터를 API에서 조회합니다. */
async function loadData() {
  if (!startDate.value || !endDate.value || !selectedTagIds.value.length) return

  loading.value = true
  try {
    const res = await fetchTagAggregate(startDate.value, endDate.value, selectedTagIds.value)
    aggregateData.value = res.data
    buildLineChart(res.data.monthlyTrend)
  } catch (e) {
    console.error('태그 합산 조회 실패', e)
  } finally {
    loading.value = false
  }
}

/** ECharts 월간 추이 Line 차트 옵션을 구성합니다. */
function buildLineChart(trend) {
  if (!trend.series.length) {
    chartOption.value = null
    return
  }

  const seriesData = trend.series.map(s => ({
    name: s.tagName,
    type: 'line',
    smooth: true,
    areaStyle: { opacity: 0.15 },
    data: s.data.map(Number),
    itemStyle: { color: s.tagColor },
    lineStyle: { color: s.tagColor },
    emphasis: { focus: 'series' }
  }))

  chartOption.value = {
    tooltip: {
      trigger: 'axis',
      formatter: (params) => {
        let html = `<strong>${params[0].axisValue}</strong><br/>`
        params.forEach(p => {
          html += `${p.marker} ${p.seriesName}: ${formatAmount(p.value)}<br/>`
        })
        return html
      }
    },
    legend: {
      bottom: 0,
      data: trend.series.map(s => s.tagName)
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '15%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: trend.months
    },
    yAxis: {
      type: 'value',
      axisLabel: {
        formatter: (val) => {
          if (val >= 10000) return Math.round(val / 10000) + '만'
          return val.toLocaleString()
        }
      }
    },
    series: seriesData
  }
}

onMounted(() => {
  loadTags()
})
</script>
