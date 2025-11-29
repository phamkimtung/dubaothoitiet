package com.example.weatherapp.service;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import okhttp3.*;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class ClothingSuggestionService {
    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent";
    private static final String API_KEY = "AIzaSyC_ORX2gL9wUr0_g62zyQN5_L5ZgUux9PI";

    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();

    private static final Gson gson = new Gson();

    public static String getClothingSuggestion(double temperature, String weatherCondition, String location, Context context) {
        Log.d("AI_DEBUG", "=== START AI CLOTHING SUGGESTION ===");
        Log.d("AI_DEBUG", "Temperature: " + temperature + "°C");
        Log.d("AI_DEBUG", "Weather: " + weatherCondition);
        Log.d("AI_DEBUG", "Location: " + location);

        // Kiểm tra kết nối mạng
        if (!isNetworkAvailable(context)) {
            Log.e("AI_DEBUG", "No network connection");
            return "❌ Không có kết nối mạng. Vui lòng kiểm tra kết nối internet và thử lại.\n\n" +
                    getFallbackSuggestion(temperature, weatherCondition);
        }

        try {
            String prompt = createPrompt(temperature, weatherCondition, location);
            Log.d("AI_DEBUG", "Prompt created: " + prompt);
            String result = callGeminiAPI(prompt);
            Log.d("AI_DEBUG", "=== END AI CLOTHING SUGGESTION ===");
            return result;
        } catch (Exception e) {
            Log.e("AI_DEBUG", "Error in AI call: " + e.getMessage(), e);
            Log.d("AI_DEBUG", "=== END AI CLOTHING SUGGESTION ===");
            return "⚠️ Không thể kết nối đến AI. Đang sử dụng gợi ý dự phòng...\n\n" +
                    getFallbackSuggestion(temperature, weatherCondition);
        }
    }

    private static String createPrompt(double temperature, String weatherCondition, String location) {
        // Prompt đơn giản hơn để test
        return "Hãy đưa ra gợi ý trang phục phù hợp cho thời tiết sau:\n" +
                "- Nhiệt độ: " + String.format("%.1f", temperature) + "°C\n" +
                "- Thời tiết: " + weatherCondition + "\n" +
                "- Địa điểm: " + location + "\n\n" +
                "Trả lời bằng tiếng Việt, ngắn gọn và thực tế.";
    }

    private static String callGeminiAPI(String prompt) throws IOException {
        Log.d("AI_DEBUG", "=== CALLING GEMINI API ===");

        // Tạo JSON request body cực kỳ đơn giản
        JsonObject requestBody = new JsonObject();

        // Chỉ dùng contents với prompt đơn giản
        JsonArray contentsArray = new JsonArray();
        JsonObject contentObject = new JsonObject();
        JsonArray partsArray = new JsonArray();
        JsonObject partObject = new JsonObject();
        partObject.addProperty("text", prompt);
        partsArray.add(partObject);
        contentObject.add("parts", partsArray);
        contentsArray.add(contentObject);
        requestBody.add("contents", contentsArray);

        String jsonBody = gson.toJson(requestBody);

        Log.d("AI_DEBUG", "API URL: " + GEMINI_API_URL);
        Log.d("AI_DEBUG", "Request JSON: " + jsonBody);

        RequestBody body = RequestBody.create(
                jsonBody,
                MediaType.parse("application/json; charset=utf-8")
        );

        String fullUrl = GEMINI_API_URL + "?key=" + API_KEY;
        Log.d("AI_DEBUG", "Full URL: " + fullUrl.replace(API_KEY, "API_KEY_HIDDEN"));

        Request request = new Request.Builder()
                .url(fullUrl)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            Log.d("AI_DEBUG", "Response Code: " + response.code());
            Log.d("AI_DEBUG", "Response Message: " + response.message());

            if (response.body() == null) {
                Log.e("AI_DEBUG", "Response body is null");
                throw new IOException("Response body is null");
            }

            String responseBody = response.body().string();
            Log.d("AI_DEBUG", "Raw Response Length: " + responseBody.length());
            Log.d("AI_DEBUG", "Raw Response (first 500 chars): " +
                    (responseBody.length() > 500 ? responseBody.substring(0, 500) + "..." : responseBody));

            if (response.isSuccessful()) {
                Log.d("AI_DEBUG", "API call successful");
                return parseGeminiResponse(responseBody);
            } else {
                Log.e("AI_DEBUG", "API call failed with code: " + response.code());
                Log.e("AI_DEBUG", "Error response: " + responseBody);

                // Thử parse error response để có thông tin chi tiết
                try {
                    JsonObject errorResponse = gson.fromJson(responseBody, JsonObject.class);
                    if (errorResponse.has("error") && errorResponse.get("error").isJsonObject()) {
                        JsonObject error = errorResponse.getAsJsonObject("error");
                        if (error.has("message")) {
                            String errorMessage = error.get("message").getAsString();
                            throw new IOException("API Error: " + errorMessage);
                        }
                    }
                } catch (Exception e) {
                    // Ignore parse error, use generic message
                }

                throw new IOException("API call failed: " + response.code() + " - " + response.message());
            }
        }
    }

    private static String parseGeminiResponse(String responseBody) {
        Log.d("AI_DEBUG", "=== PARSING RESPONSE ===");

        try {
            // Sử dụng Gson trực tiếp để parse
            JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);

            Log.d("AI_DEBUG", "All response keys: " + jsonResponse.keySet());
            Log.d("AI_DEBUG", "Full response: " + jsonResponse.toString());

            // Kiểm tra lỗi trước
            if (jsonResponse.has("error")) {
                JsonObject error = jsonResponse.getAsJsonObject("error");
                String errorMessage = "Unknown error";
                if (error.has("message")) {
                    errorMessage = error.get("message").getAsString();
                }
                Log.e("AI_DEBUG", "API Error: " + errorMessage);
                return "Lỗi từ AI: " + errorMessage;
            }

            // THỬ CÁC CẤU TRÚC RESPONSE KHÁC NHAU

            // Cấu trúc 1: candidates -> content -> parts -> text
            if (jsonResponse.has("candidates")) {
                JsonArray candidates = jsonResponse.getAsJsonArray("candidates");
                Log.d("AI_DEBUG", "Found candidates, count: " + candidates.size());

                if (candidates.size() > 0) {
                    JsonObject candidate = candidates.get(0).getAsJsonObject();
                    Log.d("AI_DEBUG", "Candidate keys: " + candidate.keySet());

                    // Kiểm tra finishReason
                    if (candidate.has("finishReason")) {
                        String finishReason = candidate.get("finishReason").getAsString();
                        Log.d("AI_DEBUG", "Finish reason: " + finishReason);
                        if ("SAFETY".equals(finishReason)) {
                            return "Nội dung bị chặn do vi phạm chính sách an toàn.";
                        }
                    }

                    // Thử lấy content -> parts -> text
                    if (candidate.has("content")) {
                        JsonObject content = candidate.getAsJsonObject("content");
                        Log.d("AI_DEBUG", "Content keys: " + content.keySet());

                        if (content.has("parts")) {
                            JsonArray parts = content.getAsJsonArray("parts");
                            if (parts.size() > 0) {
                                JsonObject part = parts.get(0).getAsJsonObject();
                                if (part.has("text")) {
                                    String text = part.get("text").getAsString();
                                    Log.d("AI_DEBUG", "Successfully extracted text from parts");
                                    return cleanResponse(text);
                                }
                            }
                        }

                        // Thử lấy text trực tiếp từ content
                        if (content.has("text")) {
                            String text = content.get("text").getAsString();
                            Log.d("AI_DEBUG", "Successfully extracted text directly from content");
                            return cleanResponse(text);
                        }
                    }
                }
            }

            // Cấu trúc 2: Trực tiếp có text trong response
            if (jsonResponse.has("text")) {
                String text = jsonResponse.get("text").getAsString();
                Log.d("AI_DEBUG", "Successfully extracted text directly from response");
                return cleanResponse(text);
            }

            // Cấu trúc 3: Có data field
            if (jsonResponse.has("data")) {
                JsonObject data = jsonResponse.getAsJsonObject("data");
                if (data.has("text")) {
                    String text = data.get("text").getAsString();
                    Log.d("AI_DEBUG", "Successfully extracted text from data field");
                    return cleanResponse(text);
                }
            }

            // Cấu trúc 4: Có result field
            if (jsonResponse.has("result")) {
                JsonObject result = jsonResponse.getAsJsonObject("result");
                if (result.has("text")) {
                    String text = result.get("text").getAsString();
                    Log.d("AI_DEBUG", "Successfully extracted text from result field");
                    return cleanResponse(text);
                }
            }

            // Cấu trúc 5: Có choices field (giống OpenAI)
            if (jsonResponse.has("choices")) {
                JsonArray choices = jsonResponse.getAsJsonArray("choices");
                if (choices.size() > 0) {
                    JsonObject choice = choices.get(0).getAsJsonObject();
                    if (choice.has("message")) {
                        JsonObject message = choice.getAsJsonObject("message");
                        if (message.has("content")) {
                            String text = message.get("content").getAsString();
                            Log.d("AI_DEBUG", "Successfully extracted text from choices->message->content");
                            return cleanResponse(text);
                        }
                    }
                    if (choice.has("text")) {
                        String text = choice.get("text").getAsString();
                        Log.d("AI_DEBUG", "Successfully extracted text from choices->text");
                        return cleanResponse(text);
                    }
                }
            }

            // Nếu không tìm thấy cấu trúc nào phù hợp, log toàn bộ response để debug
            Log.e("AI_DEBUG", "No recognizable response structure found");
            Log.e("AI_DEBUG", "Full response for analysis: " + jsonResponse.toString());

            return "Lỗi: Không thể đọc phản hồi từ AI. Cấu trúc response không nhận dạng được.\n\n" +
                    "Response keys: " + jsonResponse.keySet();

        } catch (Exception e) {
            Log.e("AI_DEBUG", "Parse error: " + e.getMessage(), e);
            return "Lỗi phân tích phản hồi: " + e.getMessage();
        }
    }

    private static String cleanResponse(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "Không nhận được phản hồi từ AI.";
        }

        // Làm sạch response
        String cleaned = text.trim();

        // Loại bỏ các ký tự đặc biệt không cần thiết
        cleaned = cleaned.replace("**", "")
                .replace("*", "•")
                .replace("\\n", "\n")
                .replace("  ", " ");

        Log.d("AI_DEBUG", "Cleaned response: " + cleaned);
        return cleaned;
    }

    private static boolean isNetworkAvailable(Context context) {
        if (context == null) {
            Log.e("AI_DEBUG", "Context is null");
            return false;
        }

        try {
            ConnectivityManager connectivityManager =
                    (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connectivityManager != null) {
                NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
                boolean isConnected = activeNetworkInfo != null && activeNetworkInfo.isConnected();
                Log.d("AI_DEBUG", "Network available: " + isConnected);
                return isConnected;
            }
        } catch (Exception e) {
            Log.e("AI_DEBUG", "Network check error: " + e.getMessage());
        }
        return false;
    }

    private static String getFallbackSuggestion(double temperature, String weatherCondition) {
        Log.d("AI_DEBUG", "Using fallback suggestion");

        StringBuilder suggestion = new StringBuilder();
        suggestion.append("👗 Gợi ý trang phục dự phòng:\n\n");

        if (temperature > 30) {
            suggestion.append("🌞 TRỜI NÓNG (>30°C):\n");
            suggestion.append("• Áo thun cotton, áo ba lỗ, quần short\n");
            suggestion.append("• Váy mỏng, chất liệu thoáng mát\n");
            suggestion.append("• Mũ rộng vành, kính râm\n");
            suggestion.append("• Kem chống nắng SPF 50+\n");
            suggestion.append("• Uống 2-3 lít nước/ngày\n\n");
        } else if (temperature > 25) {
            suggestion.append("😊 THỜI TIẾT ẤM (25-30°C):\n");
            suggestion.append("• Áo thun ngắn tay, quần dài cotton\n");
            suggestion.append("• Váy ngắn, áo sơ mi nhẹ\n");
            suggestion.append("• Mang theo áo khoác mỏng\n");
            suggestion.append("• Giày thể thao hoặc sandal\n\n");
        } else if (temperature > 20) {
            suggestion.append("🍃 MÁT MẺ (20-25°C):\n");
            suggestion.append("• Áo thun dài tay, áo len mỏng\n");
            suggestion.append("• Quần jeans hoặc quần dài\n");
            suggestion.append("• Áo khoác nhẹ hoặc cardigan\n");
            suggestion.append("• Giày kín, tất mỏng\n\n");
        } else if (temperature > 15) {
            suggestion.append("❄️ HƠI LẠNH (15-20°C):\n");
            suggestion.append("• Áo len dày, áo nỉ\n");
            suggestion.append("• Quần dày, có thể mặc 2 lớp\n");
            suggestion.append("• Áo khoác mỏng đến trung bình\n");
            suggestion.append("• Khăn quàng cổ, mũ len\n\n");
        } else if (temperature > 10) {
            suggestion.append("🧥 LẠNH (10-15°C):\n");
            suggestion.append("• Áo len dày, áo giữ nhiệt\n");
            suggestion.append("• Áo khoác dày, quần chất liệu ấm\n");
            suggestion.append("• Găng tay, khăn quàng cổ\n");
            suggestion.append("• Mũ ấm, giày bít kín\n\n");
        } else {
            suggestion.append("🥶 RẤT LẠNH (<10°C):\n");
            suggestion.append("• Nhiều lớp áo (2-3 lớp)\n");
            suggestion.append("• Áo khoác chống gió, chống nước\n");
            suggestion.append("• Găng tay dày, khăn len, mũ ấm\n");
            suggestion.append("• Tất dày, giày ấm, bảo vệ tai mũi\n\n");
        }

        String lowerCondition = weatherCondition.toLowerCase();
        if (lowerCondition.contains("rain") || lowerCondition.contains("mưa")) {
            suggestion.append("🌧️ DO CÓ MƯA:\n");
            suggestion.append("• Áo mưa hoặc áo khoác chống nước\n");
            suggestion.append("• Ô, giày/dép chống trơn\n");
        } else if (lowerCondition.contains("sun") || lowerCondition.contains("nắng")) {
            suggestion.append("☀️ DO CÓ NẮNG:\n");
            suggestion.append("• Kem chống nắng SPF 30+\n");
            suggestion.append("• Kính râm chống UV\n");
        } else if (lowerCondition.contains("wind") || lowerCondition.contains("gió")) {
            suggestion.append("💨 DO CÓ GIÓ:\n");
            suggestion.append("• Áo khoác chống gió\n");
        }

        suggestion.append("\n💡 Đây là gợi ý dự phòng. Kết nối AI sẽ cung cấp gợi ý chi tiết hơn.");

        return suggestion.toString();
    }
}