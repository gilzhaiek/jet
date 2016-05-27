#! /usr/bin/octave --persist
function ytickunit(unit)
set(gca,'yticklabel',strcat(get(gca,'yticklabel'),unit))
endfunction

function xtickunit(unit)
set(gca,'xticklabel',strcat(get(gca,'xticklabel'),unit))
endfunction

function improveplot()
FS = findall(gca,'-property','FontSize');
set(FS,'FontSize',18);
grid on
h=get (gcf, "currentaxes");
set(h,"fontweight","bold","linewidth",2)
copied_legend = findobj(gcf(),"type","axes","Tag","legend");
set(copied_legend, "FontSize", 18);
endfunction

arg_list = argv ();
if (nargin != 1)
    printf("Usage: %s <filename>",program_name ());
    exit
end
filename = arg_list{1};
if (exist(filename) != 2)
  printf ("%s cannot be found", filename);
  exit
end
data = csvread(filename)
data = data((data<=0)(:,3),:) # this cryptic statement removes any column with a negative number (ie. charging)
data(:,1)=data(:,1)-data(1,1)
data(:,1)=data(:,1)/1000;
figure('Position',[0,0,800,1000])
subplot(2,1,1)
plot(data(:,1),data(:,2),'linewidth',2)
improveplot
ytickunit(' mV')
xt=get(gca,'xticklabel');
newxt=[];
for x=1:max(size(xt))
  t = str2num(xt{x})/86400;
  newxt=[newxt; datestr(t,'HH:MM:SS')];
end
set(gca,'xticklabel',newxt);
title("Voltage level")
subplot(2,1,2)
plot(data(:,1),-1*data(:,3),'linewidth',2)
hold all
#plot(data(:,1),-1*data(:,4),'linewidth',2)
#legend2 = sprintf("Average: %i mA", -mean(data(:,3)));
#legend("Instananeous",legend2);
improveplot
ytickunit(" mA")
set(gca,'xticklabel',newxt);
title("Current consumption")
pause
#print("-S1000x800",strcat(filename,".png"))
