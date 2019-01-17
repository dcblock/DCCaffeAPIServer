## CaffeAPIServer
DigiCAP Caffe API Server

## 코딩 가이드
- ~Sun 코딩 가이드를 준수~
- ~tab은 사용하지 않고 4 space를 사용~
- ~한 줄의 최대 길이 제한 없습니다(개발툴에서 Word Wrap 사용)~
- [google java style guide](https://google.github.io/styleguide/javaguide.html)를 준수

## Push Guide
- ~개발자들은 master에 직접 개발, push 하지 않습니다.~
- ~별도의 브랜치를 생성하여 담당 영역을 개발합니다.~
- ~예를 들면, 저는 'shseo' 브랜치를 생성하고 여기에서 개발하고 push합니다.~
- ~master에 merge는 제가 진행하겠습니다.~
- ~수홍 과장은 master 새로운 push 올라오면 자신의 브런치에 merge를 하면 됩니다.~
- gitignore는 각자 생성하여 사용하고 서버에 push하지 않음
- 개발툴에서 사용하는 각종 설정정보도 push하지 않음
- 개발 담당 변경으로 master branch에 push

## 개발 환경
- Spring Boot 2.1.0 Release
- Openjdk 1.8

## Eclipse / STS 환경 설정
- lombok 라이브러리를 사용하기 때문에 별도의 개발툴 환경 설정이 필요
- Eclipse / STS: [바로가기](http://countryxide.tistory.com/16)
- IDEA: [바로가기](http://blog.woniper.net/229)
