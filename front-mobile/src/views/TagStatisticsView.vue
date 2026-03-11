<template>
  <div>
    <!-- 기간 선택 -->
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

        <v-btn color="primary" variant="tonal" block @click="loadData" :loading="loading">
          조회
        </v-btn>
      </v-card-text>
    </v-card>

    <!-- 로딩 -->
    <div v-if="loading" class="text-center pa-8">
      <v-progress-circular indeterminate color="primary" />
    </div>

    <template v-else-if="summaryData">
      <!-- Nightingale Rose 차트 -->
      <v-card>
        <v-card-title class="text-body-1 font-weight-bold py-2">주요 태그별 지출 금액</v-card-title>
        <v-card-text>
          <v-chart
            v-if="chartOption"
            :option="chartOption"
            autoresize
            style="height: 380px"
          />
          <div v-else class="text-center text-grey pa-8">데이터가 없습니다</div>
          <div class="text-caption text-grey mt-2">
            * MAIN 태그 기준 · 복수 태그 거래로 합계가 총 지출액을 초과할 수 있습니다
          </div>
        </v-card-text>
      </v-card>
    </template>

    <!-- 데이터 없음 -->
    <v-card v-else-if="!loading">
      <v-card-text class="text-center text-grey pa-8">
        기간을 선택하고 조회 버튼을 눌러주세요
      </v-card-text>
    </v-card>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { fetchTagStatisticsSummary } from '../api'

const loading = ref(false)
const summaryData = ref(null)

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

/** 태그 통계 데이터를 API에서 조회합니다. (MAIN 태그 전용) */
async function loadData() {
  if (!startDate.value || !endDate.value) return

  loading.value = true
  try {
    const summaryRes = await fetchTagStatisticsSummary(startDate.value, endDate.value, 'MAIN')
    summaryData.value = summaryRes.data
    buildRoseChart(summaryRes.data)
  } catch (e) {
    console.error('태그 통계 로딩 실패', e)
  } finally {
    loading.value = false
  }
}

/** ECharts Nightingale Rose 차트 옵션을 구성합니다. */
function buildRoseChart(data) {
  if (!data.tags.length) {
    chartOption.value = null
    return
  }

  /** 태그 데이터를 Rose 차트용 시리즈 데이터로 변환 */
  const seriesData = data.tags.map(t => ({
    value: Number(t.totalAmount),
    name: t.tagName,
    itemStyle: { color: t.tagColor }
  }))

  /** 미분류 금액이 있으면 추가 */
  if (Number(data.untaggedAmount) > 0) {
    seriesData.push({
      value: Number(data.untaggedAmount),
      name: '미분류',
      itemStyle: { color: '#9E9E9E' }
    })
  }

  chartOption.value = {
    tooltip: {
      trigger: 'item',
      formatter: (params) => {
        return `${params.name}<br/>${formatAmount(params.value)} (${params.percent}%)`
      }
    },
    legend: {
      bottom: 0,
      left: 'center',
      textStyle: { fontSize: 11 }
    },
    series: [
      {
        type: 'pie',
        radius: ['15%', '70%'],
        center: ['50%', '45%'],
        roseType: 'area',
        itemStyle: {
          borderRadius: 6
        },
        label: {
          show: true,
          formatter: (params) => {
            if (params.value >= 10000) {
              return `${params.name}\n${Math.round(params.value / 10000)}만원`
            }
            return `${params.name}\n${params.value.toLocaleString()}원`
          },
          fontSize: 11
        },
        data: seriesData
      }
    ]
  }
}

onMounted(() => {
  loadData()
})
</script>
