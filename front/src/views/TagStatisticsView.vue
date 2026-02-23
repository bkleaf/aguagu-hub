<template>
  <div>
    <!-- 기간 선택 툴바 -->
    <v-card class="mb-4">
      <v-card-text>
        <v-row align="center" dense>
          <v-col cols="12" sm="auto">
            <v-btn-toggle v-model="selectedPreset" mandatory density="compact" color="primary">
              <v-btn v-for="preset in presets" :key="preset.value" :value="preset.value" size="small">
                {{ preset.label }}
              </v-btn>
            </v-btn-toggle>
          </v-col>
          <v-col cols="12" sm="auto" class="d-flex align-center ga-2">
            <v-text-field
              v-model="startDate"
              type="date"
              label="시작일"
              density="compact"
              hide-details
              style="max-width: 180px"
              @change="selectedPreset = 'custom'"
            />
            <span class="text-grey">~</span>
            <v-text-field
              v-model="endDate"
              type="date"
              label="종료일"
              density="compact"
              hide-details
              style="max-width: 180px"
              @change="selectedPreset = 'custom'"
            />
          </v-col>
          <v-col cols="auto">
            <v-btn color="primary" variant="tonal" @click="loadData" :loading="loading">
              조회
            </v-btn>
          </v-col>
        </v-row>
      </v-card-text>
    </v-card>

    <!-- 로딩 -->
    <div v-if="loading" class="text-center pa-8">
      <v-progress-circular indeterminate color="primary" />
    </div>

    <template v-else-if="summaryData">
      <!-- 태그 요약 카드 -->
      <v-row class="mb-4">
        <v-col v-for="tag in summaryData.tags" :key="tag.tagId" cols="12" sm="6" md="4" lg="3">
          <v-card>
            <v-card-title class="d-flex align-center ga-2">
              <v-icon :color="tag.tagColor" size="small">mdi-circle</v-icon>
              {{ tag.tagName }}
              <v-spacer />
              <v-chip size="x-small" variant="outlined">{{ tag.percentage }}%</v-chip>
            </v-card-title>
            <v-card-text>
              <div class="text-h6 font-weight-bold">{{ formatAmount(tag.totalAmount) }}</div>
              <div class="text-caption text-grey">
                {{ tag.transactionCount }}건 · 평균 {{ formatAmount(tag.averageAmount) }}
              </div>
            </v-card-text>
          </v-card>
        </v-col>
        <!-- 미분류 카드 -->
        <v-col cols="12" sm="6" md="4" lg="3">
          <v-card variant="outlined">
            <v-card-title class="d-flex align-center ga-2">
              <v-icon color="grey" size="small">mdi-circle-outline</v-icon>
              미분류
            </v-card-title>
            <v-card-text>
              <div class="text-h6 font-weight-bold text-grey">{{ formatAmount(summaryData.untaggedAmount) }}</div>
              <div class="text-caption text-grey">태그 미지정 거래</div>
            </v-card-text>
          </v-card>
        </v-col>
      </v-row>

      <!-- Brush Column Chart -->
      <v-card>
        <v-card-title>주요 태그별 지출 금액</v-card-title>
        <v-card-text>
          <v-chart
            v-if="chartOption"
            :option="chartOption"
            autoresize
            style="height: 400px"
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

// 기간 선택
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
      startDate.value = formatDate(new Date(y, m, 1))
      endDate.value = formatDate(new Date(y, m + 1, 0))
      break
    case 'lastMonth':
      startDate.value = formatDate(new Date(y, m - 1, 1))
      endDate.value = formatDate(new Date(y, m, 0))
      break
    case '3months':
      startDate.value = formatDate(new Date(y, m - 2, 1))
      endDate.value = formatDate(new Date(y, m + 1, 0))
      break
    case '6months':
      startDate.value = formatDate(new Date(y, m - 5, 1))
      endDate.value = formatDate(new Date(y, m + 1, 0))
      break
    case 'thisYear':
      startDate.value = formatDate(new Date(y, 0, 1))
      endDate.value = formatDate(new Date(y, 11, 31))
      break
  }
}, { immediate: true })

// ECharts 옵션
const chartOption = ref(null)

/** 금액 포맷팅 (원 단위 3자리 콤마) */
function formatAmount(v) {
  return Number(v).toLocaleString('ko-KR') + '원'
}

/** Date 객체를 yyyy-MM-dd 형식 문자열로 변환합니다. */
function formatDate(d) {
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
    buildBrushColumnChart(summaryRes.data)
  } catch (e) {
    console.error('태그 통계 로딩 실패', e)
  } finally {
    loading.value = false
  }
}

/** ECharts Brush Select Column Chart 옵션을 구성합니다. */
function buildBrushColumnChart(data) {
  if (!data.tags.length) {
    chartOption.value = null
    return
  }

  const tagNames = data.tags.map(t => t.tagName)
  const amounts = data.tags.map(t => Number(t.totalAmount))
  const colors = data.tags.map(t => t.tagColor)

  chartOption.value = {
    toolbox: {
      feature: {
        brush: {
          type: ['lineX', 'clear'],
          title: { lineX: '범위 선택', clear: '선택 해제' }
        }
      }
    },
    brush: {
      xAxisIndex: 0,
      brushLink: 'all',
      outOfBrush: {
        colorAlpha: 0.3
      }
    },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (params) => {
        const p = params[0]
        return `${p.name}<br/>${formatAmount(p.value)}`
      }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '10%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: tagNames,
      axisLabel: {
        rotate: tagNames.length > 6 ? 30 : 0,
        interval: 0
      }
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
    series: [
      {
        type: 'bar',
        data: amounts.map((v, i) => ({
          value: v,
          itemStyle: { color: colors[i] }
        })),
        barMaxWidth: 60,
        label: {
          show: true,
          position: 'top',
          formatter: (p) => {
            if (p.value >= 10000) return Math.round(p.value / 10000) + '만'
            return p.value.toLocaleString()
          },
          fontSize: 11
        }
      }
    ]
  }
}

onMounted(() => {
  loadData()
})
</script>
