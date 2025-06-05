# DB2025Team04

### Eclipse에서 프로젝트 파일 여는 방법
1. Eclipse를 실행합니다.
2. 상단 메뉴에서 file > import를 선택합니다.
3. "General" 카테고리에서 "Existing Projects into Workspace"를 선택하고 "Next"를 클릭합니다.
4. "Select root directory"에서 프로젝트 폴더(DB2025Team04)를 선택합니다.
5. DB2025Team04 프로젝트가 목록에 나타나면 선택하고 "Finish"를 클릭합니다.
6. src 폴더 아래에서 Main.java 파일을 찾아 우클릭하고 "Run As" > "Java Application"을 선택하여 실행합니다.

### ⚠️주의사항
프로젝트에 JDBC(MySQL) 연결이 포함되어 있습니다.

### MySQL JDBC 드라이버 설정
1. <https://downloads.mysql.com/archives/c-j/> 에서 Platform Independent > ZIP Archive 다운로드하고 압축을 풉니다.
2. Eclipse에서 project > Properties > Java Build Path > Libraries 탭으로 이동합니다.
3. classpath를 선택 후 "Add External JARs..." 버튼을 클릭하여 .jar 파일을 추가합니다.
4. "Apply and Close"를 클릭하여 설정을 저장합니다.
