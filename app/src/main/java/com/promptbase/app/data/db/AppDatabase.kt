package com.promptbase.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.promptbase.app.data.model.Prompt
import com.promptbase.app.data.model.PromptTagCrossRef
import com.promptbase.app.data.model.Tag

@Database(
    entities = [Prompt::class, Tag::class, PromptTagCrossRef::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun promptDao(): PromptDao
}

internal val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS prompts_new (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `content` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `deletedAt` INTEGER DEFAULT NULL, `syncStatus` INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(`id`))")
        db.execSQL("INSERT INTO prompts_new (id, title, content, createdAt, updatedAt, deletedAt, syncStatus) SELECT id, title, content, createdAt, updatedAt, CASE WHEN isArchived = 1 THEN updatedAt ELSE NULL END, syncStatus FROM prompts")
        db.execSQL("DROP TABLE prompts")
        db.execSQL("ALTER TABLE prompts_new RENAME TO prompts")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_prompts_title ON prompts(title)")
    }
}

internal val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE tags ADD COLUMN icon TEXT NOT NULL DEFAULT 'Label'")
        db.execSQL("ALTER TABLE tags ADD COLUMN colorHex TEXT NOT NULL DEFAULT '#6750A4'")
        db.execSQL("ALTER TABLE tags ADD COLUMN displayOrder INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE tags ADD COLUMN isPredefined INTEGER NOT NULL DEFAULT 0")

        val now = System.currentTimeMillis()

        val seedTags = listOf(
            arrayOf("cat_dev", "Development", "Code", "#4CAF50", "1", "1"),
            arrayOf("cat_mkt", "Marketing", "Campaign", "#FF9800", "2", "1"),
            arrayOf("cat_email", "Email", "Mail", "#2196F3", "3", "1"),
            arrayOf("cat_writing", "Writing", "Edit", "#9C27B0", "4", "1"),
            arrayOf("cat_img", "Image Generation", "Image", "#E91E63", "5", "1"),
            arrayOf("cat_prod", "Productivity", "Bolt", "#FFC107", "6", "1"),
            arrayOf("cat_biz", "Business", "Business", "#3F51B5", "7", "1"),
            arrayOf("cat_health", "Health & Wellness", "Favorite", "#F44336", "8", "1"),
            arrayOf("cat_custom", "Custom", "Extension", "#607D8B", "99", "1"),
        )
        for (tag in seedTags) {
            db.execSQL("INSERT OR IGNORE INTO tags (tagId, name, icon, colorHex, displayOrder, isPredefined) VALUES (?, ?, ?, ?, ?, ?)", tag)
        }

        val seedPrompts = listOf(
            SeedPrompt("sp1", "Code Review Assistant", "Review the following {{language}} code for bugs, performance issues, and violations of {{best_practices}}. Provide a clear list of issues with severity levels, suggested fixes, and code examples for each.\n\n```{{language}}\n{{code_snippet}}\n```\n\nFocus on: security vulnerabilities, edge cases, and maintainability.", "cat_dev"),
            SeedPrompt("sp2", "Debug & Fix Helper", "I'm getting this error: ```{{error_message}}```\n\nHere's my code:\n```{{language}}\n{{code}}\n```\n\nExplain the root cause step by step, then provide the corrected code and suggest unit tests to prevent this issue.", "cat_dev"),
            SeedPrompt("sp3", "System Architecture Planner", "Design a system for {{project_description}}. Consider: {{requirements}}. Provide:\n1. High-level architecture diagram description\n2. Recommended tech stack ({{tech_stack}})\n3. Data model overview\n4. API endpoint design\n5. Scaling strategy\n6. Potential bottlenecks", "cat_dev"),
            SeedPrompt("sp4", "Social Media Campaign", "Write a {{platform}} post about {{topic}} targeted at {{target_audience}}. The tone should be {{tone}}. Include:\n- An attention-grabbing hook\n- 3 key value propositions\n- A clear call-to-action\n- Relevant hashtags ({{hashtags}})\n\nKeep it under {{max_chars}} characters.", "cat_mkt"),
            SeedPrompt("sp5", "Ad Copy Generator", "Create compelling ad copy for {{product_name}} — {{product_description}}. Target audience: {{target_audience}}. Key differentiator: {{unique_selling_point}}.\n\nGenerate:\n1. Headline (max 30 chars)\n2. Sub-headline (max 90 chars)\n3. Body copy (2-3 sentences)\n4. Call-to-action\n5. A/B testing variant", "cat_mkt"),
            SeedPrompt("sp6", "SEO Blog Outline", "Create an SEO-optimized blog outline about {{topic}}. Target keyword: {{primary_keyword}}. Target reader: {{target_audience}}.\n\nInclude:\n- SEO title and meta description\n- H2/H3 heading structure\n- Key points per section\n- Internal linking opportunities\n- FAQ schema suggestions", "cat_mkt"),
            SeedPrompt("sp7", "Cold Email Outreach", "Write a professional cold email to {{recipient_name}} ({{recipient_role}}) at {{company_name}}. The purpose is to {{outreach_goal}}.\n\nContext: {{context}}\nTone: {{tone}}\n\nInclude:\n- Personalized opening referencing {{mutual_connection_or_interest}}\n- Clear value proposition\n- Low-friction call-to-action", "cat_email"),
            SeedPrompt("sp8", "Follow-up Sequence", "Draft a follow-up email sequence after {{previous_interaction}}. The recipient is {{recipient_name}} from {{company_name}}. We discussed {{discussion_topic}}.\n\nCreate 3 emails:\n1. Gentle reminder (2 days after)\n2. Value-add follow-up (5 days after) with {{additional_resource}}\n3. Breakup email (10 days after)", "cat_email"),
            SeedPrompt("sp9", "Blog Post Writer", "Write a {{word_count}}-word blog post about {{topic}} in a {{tone}} tone. Target audience: {{target_audience}}.\n\nStructure:\n- Introduction with a hook\n- {{number_of_sections}} main sections\n- Actionable takeaways\n- Conclusion with call-to-action", "cat_writing"),
            SeedPrompt("sp10", "Story Premise Builder", "Develop a story premise from: {{premise}}. Genre: {{genre}}. Point of view: {{pov}}.\n\nGenerate:\n1. Logline (1-2 sentences)\n2. Main character profile (name, motivation, flaw)\n3. Setting description\n4. Opening scene in {{tone}} style\n5. Central conflict", "cat_writing"),
            SeedPrompt("sp11", "Midjourney Prompt Crafter", "Create a detailed Midjourney prompt for {{subject}}. Style: {{art_style}}. Mood: {{mood}}. Lighting: {{lighting}}. Composition: {{composition}}.\n\nStructure:\n- Subject description\n- Environment background\n- Style references ({{artist_references}})\n- Technical parameters\n\nAlso provide 3 variations.", "cat_img"),
            SeedPrompt("sp12", "DALL-E Prompt Builder", "Generate an optimized image prompt for {{scene}}. Convey: {{mood_or_atmosphere}}.\n\nFormat:\n- Subject: {{subject}}\n- Action: {{action}}\n- Environment: {{environment}}\n- Lighting: {{lighting}}\n- Color palette: {{color_palette}}\n- Camera angle: {{camera_angle}}", "cat_img"),
            SeedPrompt("sp13", "Meeting Notes Summarizer", "Summarize key points from:\n\n{{transcript}}\n\nProvide:\n1. Executive summary\n2. Key decisions made\n3. Action items with owners: {{action_items}}\n4. Open questions\n5. Next meeting agenda", "cat_prod"),
            SeedPrompt("sp14", "Task Prioritization Matrix", "Help me prioritize these tasks based on urgency and importance:\n\n{{task_list}}\n\nFor each task:\n- Quadrant (Do First / Schedule / Delegate / Eliminate)\n- Time estimate\n- Dependencies\n- Suggested deadline\n\nOptimized schedule for {{available_timeframe}}.", "cat_prod"),
            SeedPrompt("sp15", "Product Launch Plan", "Create a launch plan for {{product_name}} — {{product_description}}. Target market: {{target_market}}. Launch: {{launch_date}}.\n\nInclude:\n- Pre-launch timeline\n- Launch week checklist\n- Marketing channels: {{marketing_channels}}\n- Success metrics (KPIs)\n- Budget breakdown", "cat_biz"),
            SeedPrompt("sp16", "Client Proposal Template", "Write a proposal for {{client_name}} regarding {{project_scope}}. Solution: {{solution_overview}}.\n\nStructure:\n1. Executive summary\n2. Problem statement\n3. Proposed solution\n4. Timeline: {{timeline}}\n5. Investment: {{budget}}\n6. Team: {{team_members}}\n7. Next steps", "cat_biz"),
            SeedPrompt("sp17", "Wellness Journal Prompts", "Generate {{count}} journal prompts for {{focus_area}} (gratitude, mindfulness, anxiety, goal-setting).\n\nEach prompt:\n- Open-ended and reflective\n- Takes 5-10 minutes\n- Includes a follow-up question\n- Suitable for {{time_of_day}} practice\n\nTone: {{tone}}", "cat_health"),
            SeedPrompt("sp18", "Meal Prep & Nutrition Plan", "Create a {{dietary_preference}} meal plan for {{goal}} (weight loss, muscle gain, energy).\n\nPreferences: {{food_preferences}}\nAllergies: {{allergies}}\nMeals per day: {{meals_per_day}}\n\nInclude:\n- Weekly meal overview\n- Grocery shopping list\n- Quick prep tips\n- Nutritional breakdown", "cat_health"),
        )

        for ((i, sp) in seedPrompts.withIndex()) {
            val timestamp = now - (seedPrompts.size - i) * 1000L
            db.execSQL("INSERT OR IGNORE INTO prompts (id, title, content, createdAt, updatedAt) VALUES (?, ?, ?, ?, ?)",
                arrayOf(sp.id, sp.title, sp.content, timestamp, timestamp))
            db.execSQL("INSERT OR IGNORE INTO prompt_tag_cross_ref (promptId, tagId) VALUES (?, ?)",
                arrayOf(sp.id, sp.categoryTagId))
        }
    }

}

private data class SeedPrompt(val id: String, val title: String, val content: String, val categoryTagId: String)
