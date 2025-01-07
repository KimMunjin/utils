#!/bin/bash
#./scripts/version-bump.sh 명령어로 실행권한 부여 필요

# 1. 현재 최신 태그 가져오기
current_tag=$(git describe --tags --abbrev=0 2>/dev/null || echo "v0.0.0")

# 2. 버전 넘버 추출
version=${current_tag#v}
major=$(echo $version | cut -d. -f1)
minor=$(echo $version | cut -d. -f2)
patch=$(echo $version | cut -d. -f3)

# 3. 변경 타입 선택
echo "변경 타입을 선택하세요:"
echo "1) MAJOR (호환성 깨짐)"
echo "2) MINOR (기능 추가)"
echo "3) PATCH (버그 수정)"
read -p "선택 (1-3): " choice

# 4. 새 버전 생성
case $choice in
  1) major=$((major + 1)); minor=0; patch=0;;
  2) minor=$((minor + 1)); patch=0;;
  3) patch=$((patch + 1));;
esac

new_tag="v$major.$minor.$patch"

# 5. 태그 생성 및 푸시
git tag -a $new_tag -m "Release $new_tag"
git push origin $new_tag