<template>
  <v-card max-width="600">
    <v-card-title>카드 결제 문자 등록</v-card-title>
    <v-card-text>
      <v-form ref="formRef" @submit.prevent="submit">
        <v-select
          v-model="selectedCompany"
          :items="companies"
          item-title="label"
          item-value="code"
          label="카드사"
          :rules="[v => !!v || '카드사를 선택해주세요']"
          class="mb-2"
          @update:model-value="onCompanyChange"
        />
        <v-select
          v-model="form.phoneNumber"
          :items="phoneNumberItems"
          item-title="label"
          item-value="value"
          label="전화번호"
          :rules="[v => !!v || '전화번호는 필수입니다']"
          class="mb-2"
          readonly
        />
        <v-textarea
          v-model="form.message"
          label="문자 메시지"
          placeholder="삼성9684승인&#10;전*우&#10;4,500원 일시불&#10;01/15 17:52&#10;버거킹건대입구역&#10;누적773,774"
          rows="6"
          :rules="[v => !!v || '문자 메시지는 필수입니다']"
        />
        <v-btn type="submit" color="primary" :loading="loading" block>등록</v-btn>
      </v-form>
    </v-card-text>

    <v-snackbar v-model="snackbar.show" :color="snackbar.color" timeout="3000">
      {{ snackbar.text }}
    </v-snackbar>
  </v-card>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { createTransaction, fetchSupportedCompanies } from '../api'

const formRef = ref(null)
const loading = ref(false)
const selectedCompany = ref(null)
const companiesRaw = ref([])
const form = ref({ phoneNumber: '', message: '' })
const snackbar = ref({ show: false, text: '', color: '' })

/** 카드사 셀렉트 박스 항목 (이름 + 전화번호 표시, 청구서 발신번호 포함) */
const companies = computed(() => {
  const items = []
  companiesRaw.value
    .filter(c => c.phoneNumber)
    .forEach(c => {
      items.push({
        code: c.code,
        label: `${c.name} (${c.phoneNumber})`,
        phoneNumber: c.phoneNumber,
        type: 'transaction',
      })
      if (c.billingPhoneNumber) {
        items.push({
          code: c.code + '_BILL',
          label: `${c.name} 청구서 (${c.billingPhoneNumber})`,
          phoneNumber: c.billingPhoneNumber,
          type: 'bill',
        })
      }
    })
  return items
})

/** 선택된 카드사의 전화번호 항목 */
const phoneNumberItems = computed(() => {
  const selected = companies.value.find(c => c.code === selectedCompany.value)
  if (!selected || !selected.phoneNumber) return []
  return [{ label: selected.phoneNumber, value: selected.phoneNumber }]
})

/** 카드사 선택 시 전화번호 자동 설정 */
function onCompanyChange(code) {
  const selected = companies.value.find(c => c.code === code)
  form.value.phoneNumber = selected?.phoneNumber || ''
}

/** 지원 카드사 목록 조회 */
onMounted(async () => {
  try {
    const res = await fetchSupportedCompanies()
    companiesRaw.value = res.data
  } catch (e) {
    console.error('카드사 목록 조회 실패', e)
  }
})

async function submit() {
  const { valid } = await formRef.value.validate()
  if (!valid) return

  loading.value = true
  try {
    const res = await createTransaction(form.value)
    const data = res.data
    snackbar.value = {
      show: true,
      text: data.message || '등록 완료',
      color: data.success ? 'green' : 'orange',
    }
    if (data.success) {
      form.value = { phoneNumber: form.value.phoneNumber, message: '' }
    }
  } catch (e) {
    snackbar.value = {
      show: true,
      text: e.response?.data?.message || '등록 실패',
      color: 'red',
    }
  } finally {
    loading.value = false
  }
}
</script>
