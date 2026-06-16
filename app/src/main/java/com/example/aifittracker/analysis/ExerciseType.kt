package com.example.aifittracker.analysis

enum class ExerciseType(val displayName: String, val description: String) {
    SQUAT("Squat", "Đếm số lần Squat sâu chuẩn tư thế để tập cơ đùi và mông."),
    PUSH_UP("Hít Đất (Push-up)", "Đếm số lần chống đẩy dựa trên góc gập khuỷu tay."),
    PLANK("Plank", "Tính thời gian giữ Plank chuẩn tư thế, cảnh báo lệch hông."),
    JUMPING_JACK("Nhảy Dang Tay Chân", "Nhảy dang rộng tay chân kết hợp nhịp nhàng, bài tập cardio đốt mỡ."),
    BICEP_CURL("Gập Tay Trước (Bicep Curl)", "Gập tạ tay trước, đo góc gập tay để đếm rep chuẩn form.")
}
