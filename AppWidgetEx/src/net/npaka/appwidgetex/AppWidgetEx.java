package net.npaka.appwidgetex;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;

//�z�[���E�B�W�F�b�g
public class AppWidgetEx extends AppWidgetProvider {
    //�X�V���ɌĂ΂��
    @Override
    public void onUpdate(Context context,
        AppWidgetManager appWidgetManager,int[] appWidgetIds) {
        //�z�[���E�B�W�F�b�g����������T�[�r�X�̎��s
        Intent intent=new Intent(context,AppWidgetService.class);
        context.startService(intent);
    }
}