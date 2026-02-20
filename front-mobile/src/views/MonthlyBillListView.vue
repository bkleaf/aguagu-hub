<template>
  <div>
    <!-- 필터 영역 -->
    <v-card variant="outlined" class="mb-3">
      <v-card-text class="pb-2">
        <v-select
          v-model="selectedYear"
          :items="yearOptions"
          label="년도"
          density="compact"
          hide-details
          class="mb-2"
        />
        <v-select
          v-model="selectedMonth"
          :items="monthOptions"
          item-title="label"
          item-value="value"
          label="월"
          density="compact"
          hide-details
          class="mb-2"
        />
        <v-select
          v-model="selectedCompany"
          :items="companyOptions"
          item-title="label"
          item-value="value"
          label="카드사"
          density="compact"
          hide-details
          clearable
          class="mb-2"
        />
        <v-btn color="primary" block @click="fetchData" :loading="loading">조회</v-btn>
      </v-card-text>
    </v-card>

    <!-- 합계 카드 -->
    <v-card v-if="result" variant="outlined" class="mb-3">
      <v-card-text>
        <div class="d-flex justify-space-between align-center">
          <div>
            <div class="text-caption text-grey">조회 기간</div>
            <div class="text-body-1 font-weight-bold">{{ result.year }}년 {{ result.month }}월</div>
          </div>
          <div class="text-right">
            <div class="text-caption text-grey">{{ result.bills.length }}건</div>
            <div class="text-body-1 font-weight-bold text-primary">{{ formatAmount(result.totalAmount) }}원</div>
          </div>
        </div>
      </v-card-text>
    </v-card>

    <!-- 로딩 -->
    <v-progress-linear v-if="loading" indeterminate color="primary" class="mb-2" />

    <!-- 청구서 카드 리스트 -->
    <v-card
      v-for="(bill, idx) in bills"
      :key="idx"
      class="mb-2"
      variant="outlined"
    >
      <v-card-text class="py-2">
        <div class="d-flex justify-space-between align-center">
          <div>
            <div class="text-body-2 font-weight-medium">{{ bill.cardCompanyName }}</div>
            <div class="text-caption text-grey">결제일: {{ bill.billingDate }} &middot; 기준일: {{ bill.referenceDate }}</div>
          </div>
          <div class="text-body-1 font-weight-bold">{{ formatAmount(bill.billingAmount) }}원</div>
        </div>
      </v-card-text>
    </v-card>

    <v-card v-if="!loading && result && bills.length === 0" variant="outlined">
      <v-card-text class="text-center text-grey py-6">조회된 청구서가 없습니다</v-card-text>
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

/** 년도 옵션 */
const yearOptions = computed(() => {
  const cur = now.getFullYear()
  return [cur - 2, cur - 1, cur, cur + 1]
})

/** 월 옵션 */
const monthOptions = Array.from({ length: 12 }, (_, i) => ({
  label: `${i + 1}월`,
  value: i + 1,
}))

/** 카드사 옵션 */
const companyOptions = computed(() =>
  companiesRaw.value
    .filter(c => c.billingPhoneNumber)
    .map(c => ({ label: c.name, value: c.code }))
)

/** 금액 포맷 */
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
