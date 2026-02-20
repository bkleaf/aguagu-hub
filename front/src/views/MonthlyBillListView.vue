<template>
  <div>
    <!-- 필터 영역 -->
    <v-card class="mb-4">
      <v-card-text>
        <v-row align="center">
          <v-col cols="12" sm="3">
            <v-select
              v-model="selectedYear"
              :items="yearOptions"
              label="년도"
              density="compact"
              hide-details
            />
          </v-col>
          <v-col cols="12" sm="3">
            <v-select
              v-model="selectedMonth"
              :items="monthOptions"
              item-title="label"
              item-value="value"
              label="월"
              density="compact"
              hide-details
            />
          </v-col>
          <v-col cols="12" sm="3">
            <v-select
              v-model="selectedCompany"
              :items="companyOptions"
              item-title="label"
              item-value="value"
              label="카드사"
              density="compact"
              hide-details
              clearable
            />
          </v-col>
          <v-col cols="12" sm="3">
            <v-btn color="primary" block @click="fetchData" :loading="loading">조회</v-btn>
          </v-col>
        </v-row>
      </v-card-text>
    </v-card>

    <!-- 합계 카드 -->
    <v-card class="mb-4" v-if="result">
      <v-card-text>
        <v-row>
          <v-col cols="12" sm="4">
            <div class="text-caption text-grey">조회 기간</div>
            <div class="text-h6">{{ result.year }}년 {{ result.month }}월</div>
          </v-col>
          <v-col cols="12" sm="4">
            <div class="text-caption text-grey">청구서 수</div>
            <div class="text-h6">{{ result.bills.length }}건</div>
          </v-col>
          <v-col cols="12" sm="4">
            <div class="text-caption text-grey">청구 합계</div>
            <div class="text-h6 text-primary">{{ formatAmount(result.totalAmount) }}원</div>
          </v-col>
        </v-row>
      </v-card-text>
    </v-card>

    <!-- 데이터 테이블 -->
    <v-card>
      <v-data-table
        :headers="headers"
        :items="bills"
        :loading="loading"
        no-data-text="조회된 청구서가 없습니다"
        items-per-page="10"
      >
        <template #item.billingAmount="{ item }">
          {{ formatAmount(item.billingAmount) }}원
        </template>
        <template #item.billingDate="{ item }">
          {{ item.billingDate }}
        </template>
        <template #item.referenceDate="{ item }">
          {{ item.referenceDate }}
        </template>
      </v-data-table>
    </v-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { fetchMonthlyBills, fetchSupportedCompanies } from '../api'

const loading = ref(false)
const result = ref(null)
const bills = computed(() => result.value?.bills || [])
const companiesRaw = ref([])

const now = new Date()
const selectedYear = ref(now.getFullYear())
const selectedMonth = ref(now.getMonth() + 1)
const selectedCompany = ref(null)

/** 년도 옵션 (현재 년도 기준 -2 ~ +1) */
const yearOptions = computed(() => {
  const cur = now.getFullYear()
  return [cur - 2, cur - 1, cur, cur + 1]
})

/** 월 옵션 */
const monthOptions = Array.from({ length: 12 }, (_, i) => ({
  label: `${i + 1}월`,
  value: i + 1,
}))

/** 카드사 옵션 (청구서 발신번호가 있는 카드사만) */
const companyOptions = computed(() =>
  companiesRaw.value
    .filter(c => c.billingPhoneNumber)
    .map(c => ({ label: c.name, value: c.code }))
)

/** 테이블 헤더 */
const headers = [
  { title: '카드사', key: 'cardCompanyName', sortable: true },
  { title: '결제일', key: 'billingDate', sortable: true },
  { title: '청구금액', key: 'billingAmount', sortable: true, align: 'end' },
  { title: '기준일', key: 'referenceDate', sortable: true },
]

/** 금액 포맷팅 */
function formatAmount(val) {
  if (val == null) return '0'
  return Number(val).toLocaleString('ko-KR')
}

/** 데이터 조회 */
async function fetchData() {
  loading.value = true
  try {
    const res = await fetchMonthlyBills(selectedYear.value, selectedMonth.value, selectedCompany.value)
    result.value = res.data
  } catch (e) {
    console.error('청구서 조회 실패', e)
  } finally {
    loading.value = false
  }
}

/** 지원 카드사 목록 조회 */
onMounted(async () => {
  try {
    const res = await fetchSupportedCompanies()
    companiesRaw.value = res.data
  } catch (e) {
    console.error('카드사 목록 조회 실패', e)
  }
  fetchData()
})
</script>
