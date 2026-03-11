<template>
  <div>
    <!-- 추가 메뉴 리스트 -->
    <v-list>
      <v-list-item
        prepend-icon="mdi-cash-sync"
        title="자동결제 관리"
        subtitle="자동결제 항목 등록, 수정, 삭제"
        @click="$router.push('/auto-payments')"
      >
        <template #append>
          <v-icon>mdi-chevron-right</v-icon>
        </template>
      </v-list-item>

      <v-divider />

      <v-list-item
        prepend-icon="mdi-message-text-outline"
        title="문자 등록"
        subtitle="카드 결제 문자를 직접 등록"
        @click="$router.push('/transactions/new')"
      >
        <template #append>
          <v-icon>mdi-chevron-right</v-icon>
        </template>
      </v-list-item>

      <v-divider />

      <v-list-item
        prepend-icon="mdi-tag-multiple"
        title="태그 관리"
        subtitle="거래 분류 태그 등록, 수정, 삭제"
        @click="$router.push('/tags')"
      >
        <template #append>
          <v-icon>mdi-chevron-right</v-icon>
        </template>
      </v-list-item>

      <v-divider />

      <v-list-item
        prepend-icon="mdi-chart-donut"
        title="태그 통계"
        subtitle="주요 태그별 지출 통계 조회"
        @click="$router.push('/tag-statistics')"
      >
        <template #append>
          <v-icon>mdi-chevron-right</v-icon>
        </template>
      </v-list-item>

      <v-divider />

      <v-list-item
        prepend-icon="mdi-calculator"
        title="태그 금액 합산"
        subtitle="선택 태그 합산 금액 및 월간 추이"
        @click="$router.push('/tag-amount-summary')"
      >
        <template #append>
          <v-icon>mdi-chevron-right</v-icon>
        </template>
      </v-list-item>

      <v-divider />

      <v-list-item
        prepend-icon="mdi-credit-card"
        title="카드 관리"
        subtitle="신용카드 등록, 조회, 삭제"
        @click="$router.push('/credit-cards')"
      >
        <template #append>
          <v-icon>mdi-chevron-right</v-icon>
        </template>
      </v-list-item>

      <v-divider />

      <v-list-item
        prepend-icon="mdi-monitor"
        title="PC 버전"
        subtitle="PC 어드민 페이지로 이동"
        href="/"
      >
        <template #append>
          <v-icon>mdi-chevron-right</v-icon>
        </template>
      </v-list-item>
    </v-list>
  </div>
</template>
