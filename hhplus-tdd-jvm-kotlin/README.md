# 정리

###  JUnit 5 + mokk 테스트

### TEST 작성 팁
- mokk이 필요하면 작성 후 ->  
assertThat& assertThatThrownBy 로 검증 값 체크 ->   
verify 로 호출 체크
- unit test: 클래스.   
(단일 함수의 동작과 단일 함수의 여러 결과 ex. A(가공), B1(처리), B2(처리))
- 통합 test: 전반적인 플로우?   
(A - B1,B2)
### 개인적으로 느낀 점
- 테스트 코드를 먼저 작성함으로써 완성도 높은 기능구현이 편해진다.    
- 메인 코드의 질이 올라간다.   
(예외처리?를 작성하거나, 역할 분리를 신경쓰게 된다. 예를 들자면 연계된 세개의 분기를 가진 두 모듈을 테스트할 때, 명확하게 구분이 안되면 3+3 가 아닌 3^3 가 되는 느낌이었다.)
